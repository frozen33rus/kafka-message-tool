package application.customfxwidgets.senderconfig;

import application.constants.GuiStrings;
import application.customfxwidgets.CustomFxWidgetsLoader;
import application.customfxwidgets.Displayable;
import application.customfxwidgets.TopicConfigComboBoxConfigurator;
import application.displaybehaviour.DetachableDisplayBehaviour;
import application.displaybehaviour.DisplayBehaviour;
import application.displaybehaviour.ModelConfigObjectsGuiInformer;
import application.kafka.cluster.KafkaClusterProxies;
import application.kafka.cluster.KafkaClusterProxy;
import application.logging.Logger;
import application.model.modelobjects.KafkaBrokerConfig;
import application.model.modelobjects.KafkaSenderConfig;
import application.model.modelobjects.KafkaTopicConfig;
import application.persistence.ApplicationSettings;
import application.scripting.MessageTemplateSender;
import application.utils.GuiUtils;
import application.utils.TooltipCreator;
import application.utils.ValidationStatus;
import application.utils.Validations;
import application.utils.ValidatorUtils;
import application.utils.kafka.KafkaParitionUtils;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.StatusBar;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;
import java.util.function.Consumer;


public class SenderConfigGuiController extends AnchorPane implements Displayable {
    public static final int MARGINS_SIZE = 5;
    private static final String FXML_FILE = "SenderConfigView.fxml";
    private static final int MIN_REPEAT_COUNT = 1;
    private static final int MAX_REPEAT_COUNT = 100_000;
    private final DisplayBehaviour displayBehaviour;
    private final TopicConfigComboBoxConfigurator comboBoxConfigurator;

    private final MessageTemplateSender msgTemplateSender;
    private final VirtualizedScrollPane<StyleClassedTextArea> beforeAllMessagesSharedScriptScrollPane;
    private final VirtualizedScrollPane<StyleClassedTextArea> beforeAllMessagesScriptScrollPane;
    private final VirtualizedScrollPane<StyleClassedTextArea> beforeEachMessageScriptScrollPane;
    private final VirtualizedScrollPane<StyleClassedTextArea> messageContentScrollPane;
    private final KafkaSenderConfig config;
    private final Runnable refreshCallback;
    private final ObservableList<KafkaTopicConfig> topicConfigs;
    private final MessageSenderTaskExecutor taskExecutor;
    private final StyleClassedTextArea beforeAllmessagesSharedScriptCodeArea;
    private final StyleClassedTextArea beforeAllMessagesScriptCodeArea;
    private final StyleClassedTextArea beforeEachMessagesScriptCodeArea;
    private final StyleClassedTextArea messageContentTextArea;
    private KafkaClusterProxies kafkaClusterProxies;
    private ApplicationSettings applicationSettings;
    private SentMessagesProgressNotifier sentMessagesNotifier;
    @FXML
    private TextField messageNameTextField;
    @FXML
    private ComboBox<KafkaTopicConfig> topicConfigComboBox;
    @FXML
    private Button sendMsgPushButton;
    @FXML
    private TextField messageKeyTextField;
    @FXML
    private CheckBox messageKeyCheckBox;
    @FXML
    private Spinner<Integer> repeatCountSpinner;
    @FXML
    private AnchorPane beforeAllTabAnchorPane;
    @FXML
    private AnchorPane beforeAllSharedTabAnchorPane;
    @FXML
    private AnchorPane beforeEachMsgTabAnchorPane;
    @FXML
    private AnchorPane bodyTemplateTabAnchorPane;
    @FXML
    private CheckBox sendingSimulationModeCheckBox;
    @FXML
    private Tab beforeAllMsgScriptTab;
    @FXML
    private Tab beforeEachMsgScriptTab;

    @FXML
    private Tab beforeAllMsgSharedScriptTab;

    @FXML
    private Tab bodyTemplateTab;
    @FXML
    private Tab scriptingTab;
    @FXML
    private TitledPane messageDefinitionTitledPane;
    @FXML
    private ToggleButton detachPaneButton;
    @FXML
    private Button stopSendingButton;

    @FXML
    private StatusBar notificationBar;

    public SenderConfigGuiController(KafkaSenderConfig config,
                                     AnchorPane parentPane,
                                     ModelConfigObjectsGuiInformer guiInformer,
                                     Runnable refreshCallback,
                                     ObservableList<KafkaTopicConfig> topicConfigs,
                                     MessageTemplateSender msgTemplateSender,
                                     VirtualizedScrollPane<StyleClassedTextArea> beforeAllMessagesSharedScriptScrollPane,
                                     VirtualizedScrollPane<StyleClassedTextArea> beforeAllMessagesScriptScrollPane,
                                     VirtualizedScrollPane<StyleClassedTextArea> beforeEachMessageScriptScrollPane,
                                     VirtualizedScrollPane<StyleClassedTextArea> messageContentScrollPane,
                                     KafkaClusterProxies kafkaClusterProxies,
                                     ApplicationSettings applicationSettings) throws IOException {
        this.msgTemplateSender = msgTemplateSender;

        this.beforeAllMessagesSharedScriptScrollPane = beforeAllMessagesSharedScriptScrollPane;
        this.beforeAllMessagesScriptScrollPane = beforeAllMessagesScriptScrollPane;
        this.beforeEachMessageScriptScrollPane = beforeEachMessageScriptScrollPane;

        this.kafkaClusterProxies = kafkaClusterProxies;
        this.applicationSettings = applicationSettings;

        beforeAllmessagesSharedScriptCodeArea = this.beforeAllMessagesSharedScriptScrollPane.getContent();
        beforeAllMessagesScriptCodeArea = this.beforeAllMessagesScriptScrollPane.getContent();
        beforeEachMessagesScriptCodeArea = this.beforeEachMessageScriptScrollPane.getContent();


        this.messageContentScrollPane = messageContentScrollPane;
        messageContentTextArea = this.messageContentScrollPane.getContent();


        CustomFxWidgetsLoader.loadAnchorPane(this, FXML_FILE);

        this.config = config;
        this.refreshCallback = refreshCallback;
        this.topicConfigs = topicConfigs;

        final StringExpression windowTitle = new ReadOnlyStringWrapper("Message sender configuration");
        displayBehaviour = new DetachableDisplayBehaviour(parentPane,
                                                          windowTitle,
                                                          this,
                                                          detachPaneButton.selectedProperty(),
                                                          config,
                                                          guiInformer);

        configureMessageNameTextField();
        configureMessageContentTextArea();
        configureTopicComboBox();
        configureRepeatCountSpinner();
        configureMessageKeyCheckbox();
        configureScriptsTextAreas();
        configureMessageKeyTextField();
        configureSimulationSendingCheckBox();
        createProgressNotifier();
        GuiUtils.configureComboBoxToClearSelectedValueIfItsPreviousValueWasRemoved(topicConfigComboBox);
        comboBoxConfigurator = new TopicConfigComboBoxConfigurator<>(topicConfigComboBox, config);
        comboBoxConfigurator.configure();
        taskExecutor = new MessageSenderTaskExecutor(sendMsgPushButton.disableProperty(),
                                                     stopSendingButton.disableProperty());
    }

    private void createProgressNotifier() {
        sentMessagesNotifier = new SentMessagesProgressNotifier(notificationBar);
    }

    @Override
    public void display() {
        displayBehaviour.display();
    }

    private void configureSimulationSendingCheckBox() {
        sendingSimulationModeCheckBox.setSelected(config.getSendingSimulationModeEnabled());
        config.sendingSimulationModeEnabledProperty().bind(sendingSimulationModeCheckBox.selectedProperty());
    }

    @FXML
    private void initialize() {
        bodyTemplateTab.setText(GuiStrings.MESSAGE_BODY_TEMPLATE_NAME);
        bodyTemplateTab.setTooltip(TooltipCreator.createFrom(
            GuiStrings.MESSAGE_DEFINITION_TOOL_TIP));

        scriptingTab.setText(GuiStrings.GROOVY_SCRIPTING_TAB_NAME);

        beforeAllMsgSharedScriptTab.setText(GuiStrings.BEFORE_FIRST_MSGS_SHARED_SCRIPT_TAB_NAME);
        beforeAllMsgSharedScriptTab.setTooltip(TooltipCreator.createFrom(GuiStrings.BEFORE_FIRST_MSG_SHARED_TAB_TOOLTIP));

        beforeAllMsgScriptTab.setText(GuiStrings.BEFORE_FIRST_MSGS_SCRIPT_TAB_NAME);
        beforeAllMsgScriptTab.setTooltip(TooltipCreator.createFrom(GuiStrings.BEFORE_FIRST_MSG_TAB_TOOLTIP));

        beforeEachMsgScriptTab.setText(GuiStrings.BEFORE_EACH_MSGS_SCRIPT_TAB);
        beforeEachMsgScriptTab.setTooltip(TooltipCreator.createFrom(GuiStrings.BEFORE_EACH_MSG_TAB_TOOLTIP));

        sendMsgPushButton.setText(GuiStrings.SEND_BUTTON_TEXT);
    }

    private void configureScriptsTextAreas() {

        configureTextAreaLayouts();
        configureTextAreasTextChangeActions();
    }

    private void configureTextAreasTextChangeActions() {
        final StringProperty sharedScriptProperty = applicationSettings.appSettings().runBeforeFirstMessageSharedScriptContentProperty();
        beforeAllmessagesSharedScriptCodeArea.appendText(sharedScriptProperty.get());


        beforeAllmessagesSharedScriptCodeArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue.equals(newValue) || sharedScriptProperty.get().equals(newValue)) {
                return;
            }

            sharedScriptProperty.setValue(newValue);
        });

        sharedScriptProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue.equals(newValue) || beforeAllmessagesSharedScriptCodeArea.getText().equals(newValue)) {
                return;
            }

            beforeAllmessagesSharedScriptCodeArea.replaceText(0,
                                                              beforeAllmessagesSharedScriptCodeArea.getLength(),
                                                              newValue);
        });

        beforeAllMessagesScriptCodeArea.appendText(config.runBeforeAllMessagesScriptProperty().get());
        config.runBeforeAllMessagesScriptProperty().bind(beforeAllMessagesScriptCodeArea.textProperty());

        beforeEachMessagesScriptCodeArea.appendText(config.runBeforeEachMessageScriptProperty().get());
        config.runBeforeEachMessageScriptProperty().bind(beforeEachMessagesScriptCodeArea.textProperty());
    }

    private void configureTextAreaLayouts() {
        beforeAllSharedTabAnchorPane.getChildren().clear();
        beforeAllSharedTabAnchorPane.getChildren().add(beforeAllMessagesSharedScriptScrollPane);
        GuiUtils.expandNodeToAnchorPaneBorders(beforeAllMessagesSharedScriptScrollPane, MARGINS_SIZE);

        beforeAllTabAnchorPane.getChildren().clear();
        beforeAllTabAnchorPane.getChildren().add(beforeAllMessagesScriptScrollPane);
        GuiUtils.expandNodeToAnchorPaneBorders(beforeAllMessagesScriptScrollPane, MARGINS_SIZE);


        beforeEachMsgTabAnchorPane.getChildren().clear();
        beforeEachMsgTabAnchorPane.getChildren().add(beforeEachMessageScriptScrollPane);
        GuiUtils.expandNodeToAnchorPaneBorders(beforeEachMessageScriptScrollPane, MARGINS_SIZE);
    }


    private void configureRepeatCountSpinner() {


        repeatCountSpinner.setValueFactory(new IntegerSpinnerValueFactory(MIN_REPEAT_COUNT,
                                                                          Integer.MAX_VALUE,
                                                                          config.repeatCountProperty().get()));


        final Consumer<String> repeatCountAsStringSetter = (value) -> config.setRepeatCount(Integer.valueOf(value));
        GuiUtils.configureTextFieldToAcceptOnlyValidData(repeatCountSpinner.getEditor(),
                                                         repeatCountAsStringSetter,
                                                         this::isValidRepeatNumber);
        ValidatorUtils.configureTextFieldToAcceptOnlyDecimalValues(repeatCountSpinner.getEditor());

    }

    private boolean isValidRepeatNumber(String e) {
        return StringUtils.isNumeric(e) && Integer.parseUnsignedInt(e) <= MAX_REPEAT_COUNT;
    }

    private void configureMessageNameTextField() {
        messageNameTextField.setText(config.getName());

        GuiUtils.configureTextFieldToAcceptOnlyValidData(messageNameTextField,
                                                         config::setName,
                                                         ValidatorUtils::isStringIdentifierValid,
                                                         refreshCallback);
    }

    private void configureMessageContentTextArea() {

        bodyTemplateTabAnchorPane.getChildren().clear();
        bodyTemplateTabAnchorPane.getChildren().add(messageContentScrollPane);
        GuiUtils.expandNodeToAnchorPaneBorders(messageContentScrollPane, MARGINS_SIZE);


        messageContentTextArea.appendText(config.getMsgContentTemplate());
        messageContentTextArea.textProperty().addListener((observableValue, s, t1) -> config.setMsgContentTemplate(t1));

    }

    private void configureTopicComboBox() {
        topicConfigComboBox.setItems(topicConfigs);
    }

    private void configureMessageKeyTextField() {
        messageKeyTextField.setText(config.getMessageKey());
        config.messageKeyProperty().bind(messageKeyTextField.textProperty());
        config.messageKeyProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String messageKey) {
                displayProbableAssignedPartitionForMessageSentToTopic(messageKey);
            }
        });
    }

    private void displayProbableAssignedPartitionForMessageSentToTopic(String messageKey) {
        final ValidationStatus validationStatus = Validations.validateForCalculatingPartition(config, kafkaClusterProxies);
        if (!validationStatus.isSuccess()) {
            showInfoWhyTargetPartitionCouldNotBeCalculated(validationStatus.validationFailureMessage());
            return;
        }

        final KafkaTopicConfig topicConfig = config.getRelatedConfig();
        final KafkaBrokerConfig brokerConfig = topicConfig.getRelatedConfig();
        final KafkaClusterProxy kafkaClusterProxy = kafkaClusterProxies.get(brokerConfig.getHostInfo());
        final int partitions = kafkaClusterProxy.partitionsForTopic(topicConfig.getTopicName());
        final int expectedAssignedPartitions = KafkaParitionUtils.partition(messageKey, partitions);
        Logger.info(String.format("Expected assigned partition for key '%s' is %d", messageKey, expectedAssignedPartitions));
    }

    private void showInfoWhyTargetPartitionCouldNotBeCalculated(String reason) {
        final String msgPrefix = "Could not calculate target partition for message: ";
        Logger.info(String.format("%s%s", msgPrefix, reason));
    }

    private void configureMessageKeyCheckbox() {
        messageKeyCheckBox.selectedProperty().set(config.isMessageKeyEnabled());
        messageKeyCheckBox.selectedProperty().bindBidirectional(config.messageKeyEnabledProperty());
        messageKeyTextField.disableProperty().bind(messageKeyCheckBox.selectedProperty().not());
    }

    @FXML
    private void onSendButtonClicked() {

        final KafkaTopicConfig topicConfig = config.getRelatedConfig();
        if (topicConfig == null) {
            Logger.error("Could not send msg, topic configuration is not assigned.");
            return;
        }

        final KafkaBrokerConfig brokerConfig = topicConfig.getRelatedConfig();
        if (brokerConfig == null) {
            Logger.error(String.format("Invalid topic config '%s' (broker config not assigned)",
                                       topicConfig.getTopicName()));
            return;
        }

        sentMessagesNotifier.clearMsgSentProgress();
        taskExecutor.run(() -> msgTemplateSender.send(config,
                                                      sentMessagesNotifier,
                                                      applicationSettings.appSettings().getRunBeforeFirstMessageSharedScriptContent(),
                                                      sendingSimulationModeCheckBox.isSelected()));

    }


    @FXML
    private void onStopSendingButtonClicked() {
        taskExecutor.stop();
    }
}
