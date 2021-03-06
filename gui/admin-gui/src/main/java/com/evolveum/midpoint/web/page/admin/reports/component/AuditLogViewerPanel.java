/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogDetails;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by honchar.
 */
public class AuditLogViewerPanel extends BasePanel<AuditSearchDto> {
	private static final long serialVersionUID = 1L;
	
    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String ID_TABLE = "table";
    private static final String ID_FROM = "fromField";
    private static final String ID_TO = "toField";
    private static final String ID_INITIATOR_NAME = "initiatorNameField";
    private static final String ID_TARGET_NAME_FIELD = "targetNameField";
    private static final String ID_TARGET_NAME = "targetName";
    private static final String ID_TARGET_OWNER_NAME = "targetOwnerName";
    private static final String ID_TARGET_OWNER_NAME_FIELD = "targetOwnerNameField";
    private static final String ID_CHANNEL = "channelField";
    private static final String ID_HOST_IDENTIFIER = "hostIdentifierField";
    private static final String ID_EVENT_TYPE = "eventTypeField";
    private static final String ID_EVENT_STAGE_FIELD = "eventStageField";
    private static final String ID_EVENT_STAGE = "eventStage";
    private static final String ID_OUTCOME = "outcomeField";
    private static final String ID_CHANGED_ITEM = "changedItem";
    private static final String ID_VALUE_REF_TARGET_NAMES_FIELD = "valueRefTargetNamesField";
	private static final String ID_VALUE_REF_TARGET_NAMES = "valueRefTargetNames";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_RESET_SEARCH_BUTTON = "resetSearchButton";
    private static final String ID_FEEDBACK = "feedback";

    public static final String TARGET_NAME_LABEL_VISIBILITY = "targetNameLabel";
    public static final String TARGET_NAME_FIELD_VISIBILITY = "targetFieldField";
    public static final String TARGET_OWNER_LABEL_VISIBILITY = "targetOwnerLabel";
    public static final String TARGET_OWNER_FIELD_VISIBILITY = "targetOwnerField";
    public static final String TARGET_COLUMN_VISIBILITY = "targetColumn";
    public static final String TARGET_OWNER_COLUMN_VISIBILITY = "targetOwnerColumn";
	public static final String VALUE_REF_TARGET_NAME_LABEL_VISIBILITY = "valueRefTargetNameLabel";
	public static final String VALUE_REF_TARGET_NAME_FIELD_VISIBILITY = "valueRefTargetNameField";
    public static final String EVENT_STAGE_COLUMN_VISIBILITY = "eventStageColumn";
    public static final String EVENT_STAGE_LABEL_VISIBILITY = "eventStageLabel";
    public static final String EVENT_STAGE_FIELD_VISIBILITY = "eventStageField";

    static final Trace LOGGER = TraceManager.getTrace(AuditLogViewerPanel.class);


    private static final String OPERATION_RESOLVE_REFENRENCE_NAME = AuditLogViewerPanel.class.getSimpleName()
            + ".resolveReferenceName()";

    private static final int DEFAULT_PAGE_SIZE = 10;
    private PageBase pageBase;
    private Map<String, Boolean> visibilityMap;
    protected AuditLogStorage auditLogStorage;

    public AuditLogViewerPanel(String id, Map<String, Boolean> visibilityMap){
        super(id, Model.of(new AuditSearchDto()));
        this.visibilityMap = visibilityMap;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        pageBase = getPageBase();
        setAuditSearchModelObject();
        initLayout();
    }

    private void setAuditSearchModelObject(){
        AuditSearchDto searchDto;
        if (useSessionStorageOnPanelLoad()){
            auditLogStorage = pageBase.getSessionStorage().getAuditLog();
            searchDto = auditLogStorage.getSearchDto();
        } else {
            searchDto = initAuditSearchDto();
            auditLogStorage = pageBase.getSessionStorage().getUserHistoryAuditLog();
            auditLogStorage.setSearchDto(searchDto);
        }
        getModel().setObject(searchDto);
    }

    protected AuditSearchDto initAuditSearchDto(){
        return new AuditSearchDto();
    }

    protected boolean useSessionStorageOnPanelLoad(){
        return true;
    }

    private void initLayout() {

        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        initParametersPanel(mainForm);
        initAuditLogViewerTable(mainForm);
    }

    private void initParametersPanel(Form mainForm) {
        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        mainForm.add(parametersPanel);

        PropertyModel<XMLGregorianCalendar> fromModel = new PropertyModel<XMLGregorianCalendar>(
                getModel(), AuditSearchDto.F_FROM);

        DatePanel from = new DatePanel(ID_FROM, fromModel);
        DateValidator dateFromValidator = WebComponentUtil.getRangeValidator(mainForm,
                new ItemPath(AuditSearchDto.F_FROM));
        dateFromValidator.setMessageKey("AuditLogViewerPanel.dateValidatorMessage");
        dateFromValidator.setDateFrom((DateTimeField) from.getBaseFormComponent());
        for (FormComponent<?> formComponent : from.getFormComponents()) {
            formComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }

        from.setOutputMarkupId(true);
        parametersPanel.add(from);

        PropertyModel<XMLGregorianCalendar> toModel = new PropertyModel<XMLGregorianCalendar>(getModel(),
                AuditSearchDto.F_TO);
        DatePanel to = new DatePanel(ID_TO, toModel);
        DateValidator dateToValidator = WebComponentUtil.getRangeValidator(mainForm,
                new ItemPath(AuditSearchDto.F_FROM));
        dateToValidator.setMessageKey("AuditLogViewerPanel.dateValidatorMessage");
        dateToValidator.setDateTo((DateTimeField) to.getBaseFormComponent());
        for (FormComponent<?> formComponent : to.getFormComponents()) {
            formComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }

        to.setOutputMarkupId(true);
        parametersPanel.add(to);

        PropertyModel<ItemPathDto> changedItemModel = new PropertyModel<ItemPathDto>(getModel(),
                AuditSearchDto.F_CHANGED_ITEM);

        ItemPathPanel changedItemPanel = new ItemPathPanel(ID_CHANGED_ITEM, changedItemModel, pageBase);
//        changedItemPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
//        changedItemPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        changedItemPanel.setOutputMarkupId(true);
        parametersPanel.add(changedItemPanel);

        PropertyModel<String> hostIdentifierModel = new PropertyModel<>(getModel(),
                AuditSearchDto.F_HOST_IDENTIFIER);
        TextPanel<String> hostIdentifier = new TextPanel<>(ID_HOST_IDENTIFIER, hostIdentifierModel);
        hostIdentifier.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        hostIdentifier.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        hostIdentifier.setOutputMarkupId(true);
        parametersPanel.add(hostIdentifier);

        ListModel<AuditEventTypeType> eventTypeListModel = new ListModel<AuditEventTypeType>(
                Arrays.asList(AuditEventTypeType.values()));
        PropertyModel<AuditEventTypeType> eventTypeModel = new PropertyModel<AuditEventTypeType>(
                getModel(), AuditSearchDto.F_EVENT_TYPE);
        DropDownChoicePanel<AuditEventTypeType> eventType = new DropDownChoicePanel<AuditEventTypeType>(
                ID_EVENT_TYPE, eventTypeModel, eventTypeListModel,
                new EnumChoiceRenderer<AuditEventTypeType>(), true);
        eventType.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        eventType.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        eventType.setOutputMarkupId(true);
        parametersPanel.add(eventType);

        WebMarkupContainer eventStage = new WebMarkupContainer(ID_EVENT_STAGE);
        eventStage.add(visibilityByKey(visibilityMap, EVENT_STAGE_LABEL_VISIBILITY));
        eventStage.setOutputMarkupId(true);
        parametersPanel.add(eventStage);

        ListModel<AuditEventStageType> eventStageListModel = new ListModel<AuditEventStageType>(
                Arrays.asList(AuditEventStageType.values()));
        PropertyModel<AuditEventStageType> eventStageModel = new PropertyModel<AuditEventStageType>(
                getModel(), AuditSearchDto.F_EVENT_STAGE);
        DropDownChoicePanel<AuditEventStageType> eventStageField = new DropDownChoicePanel<AuditEventStageType>(
                ID_EVENT_STAGE_FIELD, eventStageModel, eventStageListModel,
                new EnumChoiceRenderer<AuditEventStageType>(), true);
        eventStageField.add(visibilityByKey(visibilityMap, EVENT_STAGE_FIELD_VISIBILITY));
        eventStageField.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        eventStageField.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        eventStageField.setOutputMarkupId(true);
        eventStage.add(eventStageField);

        ListModel<OperationResultStatusType> outcomeListModel = new ListModel<>(Arrays.asList(OperationResultStatusType.values()));
        PropertyModel<OperationResultStatusType> outcomeModel = new PropertyModel<>(getModel(), AuditSearchDto.F_OUTCOME);
        DropDownChoicePanel<OperationResultStatusType> outcome = new DropDownChoicePanel<>(ID_OUTCOME, outcomeModel,
                outcomeListModel, new EnumChoiceRenderer<>(), true);
        outcome.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        outcome.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        outcome.setOutputMarkupId(true);
        parametersPanel.add(outcome);

        List<String> channelList = WebComponentUtil.getChannelList();
        List<QName> channelQnameList = new ArrayList<QName>();
        for (int i = 0; i < channelList.size(); i++) {
            String channel = channelList.get(i);
            if (channel != null) {
                QName channelQName = QNameUtil.uriToQName(channel);
                channelQnameList.add(channelQName);
            }
        }
        ListModel<QName> channelListModel = new ListModel<QName>(channelQnameList);
        PropertyModel<QName> channelModel = new PropertyModel<QName>(getModel(),
                AuditSearchDto.F_CHANNEL);
        DropDownChoicePanel<QName> channel = new DropDownChoicePanel<QName>(ID_CHANNEL, channelModel,
                channelListModel, new QNameChoiceRenderer(), true);
        channel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        channel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        channel.setOutputMarkupId(true);
        parametersPanel.add(channel);

        List<Class<? extends ObjectType>> allowedClasses = new ArrayList<>();
        allowedClasses.add(UserType.class);
        MultiValueChoosePanel<ObjectType> chooseInitiatorPanel = new SingleValueChoosePanel<>(
                ID_INITIATOR_NAME, allowedClasses, objectReferenceTransformer,
                new PropertyModel<>(getModel(), AuditSearchDto.F_INITIATOR_NAME));
        parametersPanel.add(chooseInitiatorPanel);

        WebMarkupContainer targetOwnerName = new WebMarkupContainer(ID_TARGET_OWNER_NAME);
        targetOwnerName.add(visibilityByKey(visibilityMap, TARGET_OWNER_LABEL_VISIBILITY));
        parametersPanel.add(targetOwnerName);

        MultiValueChoosePanel<ObjectType> chooseTargetOwnerPanel = new SingleValueChoosePanel<>(
                ID_TARGET_OWNER_NAME_FIELD, allowedClasses, objectReferenceTransformer,
                new PropertyModel<>(getModel(), AuditSearchDto.F_TARGET_OWNER_NAME));

        chooseTargetOwnerPanel.add(visibilityByKey(visibilityMap, TARGET_OWNER_FIELD_VISIBILITY));
        targetOwnerName.add(chooseTargetOwnerPanel);

        WebMarkupContainer targetName = new WebMarkupContainer(ID_TARGET_NAME);
        targetName.add(visibilityByKey(visibilityMap, TARGET_NAME_LABEL_VISIBILITY));
        parametersPanel.add(targetName);
        List<Class<? extends ObjectType>> allowedClassesAll = new ArrayList<>();
        allowedClassesAll.addAll(ObjectTypes.getAllObjectTypes());

        MultiValueChoosePanel<ObjectType> chooseTargetPanel = new MultiValueChoosePanel<ObjectType>(
        		ID_TARGET_NAME_FIELD,
        		new PropertyModel<List<ObjectType>>(getModel(), AuditSearchDto.F_TARGET_NAMES_OBJECTS),
        		allowedClassesAll);
        chooseTargetPanel.setOutputMarkupId(true);
        chooseTargetPanel.add(visibilityByKey(visibilityMap, TARGET_NAME_FIELD_VISIBILITY));
        targetName.add(chooseTargetPanel);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON,
                createStringResource("BasicSearchPanel.search")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                auditLogStorage.setSearchDto(getModel().getObject());
                searchUpdatePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(pageBase.getFeedbackPanel());
            }
        };
        searchButton.setOutputMarkupId(true);
        parametersPanel.add(searchButton);

        AjaxSubmitButton resetSearchButton = new AjaxSubmitButton(ID_RESET_SEARCH_BUTTON,
                createStringResource("AuditLogViewerPanel.resetSearchButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                getModel().setObject(initAuditSearchDto());
                auditLogStorage.setSearchDto(getModel().getObject());
                searchUpdatePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(pageBase.getFeedbackPanel());
            }
        };
        resetSearchButton.setOutputMarkupId(true);
        parametersPanel.add(resetSearchButton);

        WebMarkupContainer valueRefTargetNameContainer = new WebMarkupContainer(ID_VALUE_REF_TARGET_NAMES);
        valueRefTargetNameContainer.add(visibilityByKey(visibilityMap, TARGET_NAME_LABEL_VISIBILITY));
        parametersPanel.add(valueRefTargetNameContainer);

        MultiValueChoosePanel<ObjectType> chooseValueRefTargetNamePanel = new MultiValueChoosePanel<ObjectType>(
        		ID_VALUE_REF_TARGET_NAMES_FIELD,
        		new PropertyModel<List<ObjectType>>(getModel(), AuditSearchDto.F_VALUE_REF_TARGET_NAME),
        		allowedClassesAll);
        chooseValueRefTargetNamePanel.setOutputMarkupId(true);
        chooseValueRefTargetNamePanel.add(visibilityByKey(visibilityMap, VALUE_REF_TARGET_NAME_FIELD_VISIBILITY));
        valueRefTargetNameContainer.add(chooseValueRefTargetNamePanel);

    }

	// Serializable as it becomes part of panel which is serialized
    private Function<ObjectType, ObjectReferenceType> objectReferenceTransformer =
    		(Function<ObjectType, ObjectReferenceType> & Serializable) (ObjectType o) ->
        		ObjectTypeUtil.createObjectRef(o);

	// Serializable as it becomes part of panel which is serialized
    private Function<ObjectType, String> stringTransformer =
    		(Function<ObjectType, String> & Serializable) (ObjectType o) ->
        		o.getName().getOrig();

	private VisibleEnableBehaviour visibilityByKey(Map<String, Boolean> visibilityMap2, String visibilityKey) {
		return new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;
            @Override
	        public boolean isVisible(){
					return visibilityMap2 == null || visibilityMap2.get(visibilityKey) == null ?
	                        true : visibilityMap2.get(visibilityKey);
            }
        };
	}

    private void initAuditLogViewerTable(Form mainForm) {
        AuditEventRecordProvider provider = new AuditEventRecordProvider(AuditLogViewerPanel.this) {
            private static final long serialVersionUID = 1L;

            public Map<String, Object> getParameters() {
                Map<String, Object> parameters = new HashMap<String, Object>();

                AuditSearchDto search = AuditLogViewerPanel.this.getModelObject();
                parameters.put(AuditEventRecordProvider.PARAMETER_FROM, search.getFrom());
                parameters.put(AuditEventRecordProvider.PARAMETER_TO, search.getTo());

                if (search.getChannel() != null) {
                    parameters.put(AuditEventRecordProvider.PARAMETER_CHANNEL, QNameUtil.qNameToUri(search.getChannel()));
                }
                parameters.put(AuditEventRecordProvider.PARAMETER_HOST_IDENTIFIER, search.getHostIdentifier());

                if (search.getInitiatorName() != null) {
                    parameters.put(AuditEventRecordProvider.PARAMETER_INITIATOR_NAME, search.getInitiatorName().getOid());
                }

                if (search.getTargetOwnerName() != null) {
                    parameters.put(AuditEventRecordProvider.PARAMETER_TARGET_OWNER_NAME, search.getTargetOwnerName().getOid());
                }
                List<String> targetOids = new ArrayList<>();
                if (isNotEmpty(search.getTargetNamesObjects())) {
                    targetOids.addAll(search.getTargetNamesObjects().stream()
                    		.map(ObjectType::getOid)
                    		.collect(toList()));
                }
                if (isNotEmpty(search.getTargetNames())) {
                    targetOids.addAll(search.getTargetNames().stream()
                    		.map(ObjectReferenceType::getOid)
                    		.collect(toList()));
                }
                if (!targetOids.isEmpty()) {
    				parameters.put(AuditEventRecordProvider.PARAMETER_TARGET_NAMES, targetOids);
                }
                if (search.getChangedItem().toItemPath() != null) {
                	ItemPath itemPath = search.getChangedItem().toItemPath();
                	parameters.put(AuditEventRecordProvider.PARAMETER_CHANGED_ITEM, CanonicalItemPath.create(itemPath).asString());
                }
                parameters.put(AuditEventRecordProvider.PARAMETER_EVENT_TYPE, search.getEventType());
                parameters.put(AuditEventRecordProvider.PARAMETER_EVENT_STAGE, search.getEventStage());
                parameters.put(AuditEventRecordProvider.PARAMETER_OUTCOME, search.getOutcome());
                if (isNotEmpty(search.getvalueRefTargetNames())) {
	                parameters.put(AuditEventRecordProvider.PARAMETER_VALUE_REF_TARGET_NAMES,
	                		search.getvalueRefTargetNames().stream()
	                		.map(ObjectType::getName)
	                		.map(PolyStringType::getOrig)
	                		.collect(toList()));
                }
                return parameters;
            }

            @Override
            protected void saveCurrentPage(long from, long count) {
                if (count != 0) {
                    auditLogStorage.setPageNumber(from / count);
                }
            }

        };
        UserProfileStorage userProfile = pageBase.getSessionStorage().getUserProfile();
        int pageSize = DEFAULT_PAGE_SIZE;
        if (userProfile.getTables().containsKey(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER)) {
            pageSize = userProfile.getPagingSize(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER);
        }
        List<IColumn<AuditEventRecordType, String>> columns = initColumns();
        BoxedTablePanel<AuditEventRecordType> table = new BoxedTablePanel<AuditEventRecordType>(ID_TABLE, provider, columns,
                UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER, pageSize) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
            	CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(id) {

					private static final long serialVersionUID = 1L;

					@Override
					protected String getFilename() {
						return "AuditLogViewer_" + createStringResource("MainObjectListPanel.exportFileName").getString();
					}

					@Override
					protected DataTable<?, ?> getDataTable() {
						return getAuditLogViewerTable().getDataTable();
					}
				};

                return exportDataLink;
            }

            @Override
            public void setShowPaging(boolean show) {
                //we don't need to do anything here
            }

            };
        table.setShowPaging(true);
        table.setCurrentPage(auditLogStorage.getPageNumber());
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private BoxedTablePanel getAuditLogViewerTable(){
        return (BoxedTablePanel) get(ID_MAIN_FORM).get(ID_TABLE);
    }

    protected List<IColumn<AuditEventRecordType, String>> initColumns() {
        List<IColumn<AuditEventRecordType, String>> columns = new ArrayList<IColumn<AuditEventRecordType, String>>();
        IColumn<AuditEventRecordType, String> linkColumn = new LinkColumn<AuditEventRecordType>(
                createStringResource("AuditEventRecordType.timestamp"), "timestamp") {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(final IModel<AuditEventRecordType> rowModel){
                return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
                    public String getObject() {
                        XMLGregorianCalendar time = rowModel.getObject().getTimestamp();
                        return WebComponentUtil.formatDate(time);
                    }
                };
            }
            @Override
            public void onClick(AjaxRequestTarget target, IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType record = rowModel.getObject();
                try {
                    AuditEventRecord.adopt(record, pageBase.getPrismContext());
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't adopt event record: " + e, e);
                }
                pageBase.navigateToNext(new PageAuditLogDetails(record));
            }

        };
        columns.add(linkColumn);

        PropertyColumn<AuditEventRecordType, String> initiatorRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.initiatorRef"),
                AuditEventRecordType.F_INITIATOR_REF.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = rowModel.getObject();
                createReferenceColumn(auditEventRecordType.getInitiatorRef(), item, componentId);
            }
        };
        columns.add(initiatorRefColumn);

        if (visibilityMap == null || visibilityMap.get(EVENT_STAGE_COLUMN_VISIBILITY) == null ||
                visibilityMap.get(EVENT_STAGE_COLUMN_VISIBILITY)) {
            IColumn<AuditEventRecordType, String> eventStageColumn = new PropertyColumn<AuditEventRecordType, String>(
                    createStringResource("PageAuditLogViewer.eventStageLabel"), "eventStage");
            columns.add(eventStageColumn);
        }
        IColumn<AuditEventRecordType, String> eventTypeColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("PageAuditLogViewer.eventTypeLabel"), "eventType");
        columns.add(eventTypeColumn);

        if (visibilityMap == null || visibilityMap.get(TARGET_COLUMN_VISIBILITY) == null ||
                visibilityMap.get(TARGET_COLUMN_VISIBILITY)) {
            PropertyColumn<AuditEventRecordType, String> targetRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.targetRef"),
                    AuditEventRecordType.F_TARGET_REF.getLocalPart()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                         IModel<AuditEventRecordType> rowModel) {
                    AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                    createReferenceColumn(auditEventRecordType.getTargetRef(), item, componentId);
                }
            };
            columns.add(targetRefColumn);
        }

        if (visibilityMap == null || visibilityMap.get(TARGET_OWNER_COLUMN_VISIBILITY) == null ||
                visibilityMap.get(TARGET_OWNER_COLUMN_VISIBILITY)) {
            PropertyColumn<AuditEventRecordType, String> targetOwnerRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.targetOwnerRef"),
                    AuditEventRecordType.F_TARGET_OWNER_REF.getLocalPart()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                         IModel<AuditEventRecordType> rowModel) {
                    AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                    createReferenceColumn(auditEventRecordType.getTargetOwnerRef(), item, componentId);
                }
            };
            columns.add(targetOwnerRefColumn);
        }
        IColumn<AuditEventRecordType, String> channelColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("AuditEventRecordType.channel"), "channel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                String channel = auditEventRecordType.getChannel();
                if (channel != null) {
                    QName channelQName = QNameUtil.uriToQName(channel);
                    String return_ = channelQName.getLocalPart();
                    item.add(new Label(componentId, return_));
                } else {
                    item.add(new Label(componentId, ""));
                }
                item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
            }
        };
        columns.add(channelColumn);

        IColumn<AuditEventRecordType, String> outcomeColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("PageAuditLogViewer.outcomeLabel"), "outcome");
        columns.add(outcomeColumn);

        return columns;
    }

    private void createReferenceColumn(ObjectReferenceType ref, Item item, String componentId) {
        String name = WebModelServiceUtils.resolveReferenceName(ref, pageBase,
                pageBase.createSimpleTask(OPERATION_RESOLVE_REFENRENCE_NAME),
                new OperationResult(OPERATION_RESOLVE_REFENRENCE_NAME));
        item.add(new Label(componentId, name));
        item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
    }

    private void searchUpdatePerformed(AjaxRequestTarget target){
        auditLogStorage.setPageNumber(0);
//        Form mainForm = getMainFormComponent();
//        initAuditLogViewerTable(mainForm);
        pageBase.getFeedbackPanel().getFeedbackMessages().clear();
        target.add(pageBase.getFeedbackPanel());
        target.add(getMainFormComponent());
    }

    private Form getMainFormComponent(){
        return (Form) get(ID_MAIN_FORM);
    }
}
