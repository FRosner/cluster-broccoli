module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Dict exposing (Dict)
import Messages exposing (..)
import Utils.MaybeUtils as MaybeUtils
import Utils.CmdUtils as CmdUtils

updateBodyView : UpdateBodyViewMsg -> BodyUiModel -> (BodyUiModel, Cmd AnyMsg)
updateBodyView message oldBodyUiModel =
  let
    ( oldExpandedTemplates
    , oldSelectedInstances
    , oldExpandedInstances
    , oldInstanceParameterForms
    , oldEditInstanceVisibleSecrets
    , oldNewTemplateVisibleSecrets
    , oldExpandedNewInstanceForms
    ) =
    ( oldBodyUiModel.expandedTemplates
    , oldBodyUiModel.selectedInstances
    , oldBodyUiModel.expandedInstances
    , oldBodyUiModel.instanceParameterForms
    , oldBodyUiModel.visibleEditInstanceSecrets
    , oldBodyUiModel.visibleNewTemplateSecrets
    , oldBodyUiModel.expandedNewInstanceForms
    )
  in
    case message of
      ToggleTemplate templateId ->
        let newExpandedTemplates =
          insertOrRemove (not (Set.member templateId oldExpandedTemplates)) templateId oldExpandedTemplates
        in
          ( { oldBodyUiModel | expandedTemplates = newExpandedTemplates }
          , Cmd.none
          )
      InstanceSelected instanceId selected ->
        let newSelectedInstances =
          insertOrRemove selected instanceId oldSelectedInstances
        in
          ( { oldBodyUiModel | selectedInstances = newSelectedInstances }
          , Cmd.none
          )
      AllInstancesSelected instanceIds selected ->
        let newSelectedInstances =
          unionOrDiff selected oldSelectedInstances (Set.fromList instanceIds)
        in
          ( { oldBodyUiModel | selectedInstances = newSelectedInstances }
          , Cmd.none
          )
      InstanceExpanded instanceId expanded ->
        let newExpandedInstances =
          insertOrRemove expanded instanceId oldExpandedInstances
        in
          ( { oldBodyUiModel | expandedInstances = newExpandedInstances }
          , Cmd.none
          )
      AllInstancesExpanded instanceIds expanded ->
        let newExpandedInstances =
          unionOrDiff expanded oldExpandedInstances (Set.fromList instanceIds)
        in
          ( { oldBodyUiModel | expandedInstances = newExpandedInstances }
          , Cmd.none
          )
      EnterEditInstanceParameterValue instance parameter value ->
        let newInstanceParameterForms =
          ( Dict.update
              instance.id
              ( updateEditInstanceParameterForm instance parameter value )
              oldInstanceParameterForms
          )
        in
          ( { oldBodyUiModel | instanceParameterForms = newInstanceParameterForms }
          , Cmd.none
          )
      EnterNewInstanceParameterValue template parameter value ->
        let newExpandedNewInstanceForms =
          ( Dict.update
              template.id
              ( updateNewInstanceParameterForm parameter value )
              oldExpandedNewInstanceForms
          )
        in
          ( { oldBodyUiModel | expandedNewInstanceForms = newExpandedNewInstanceForms }
          , Cmd.none
          )
      DiscardParameterValueChanges instance ->
        ( { oldBodyUiModel | instanceParameterForms = resetEditParameterForm instance oldInstanceParameterForms }
        , Cmd.none
        )
      ApplyParameterValueChanges instance ->
        ( { oldBodyUiModel | instanceParameterForms = resetEditParameterForm instance oldInstanceParameterForms }
        , Cmd.none
        )
      ToggleEditInstanceSecretVisibility instanceId parameter ->
        let newVisibleSecrets =
          ( insertOrRemove
              ( not (Set.member (instanceId, parameter) oldEditInstanceVisibleSecrets) )
              (instanceId, parameter)
              oldEditInstanceVisibleSecrets
          )
        in
          ( { oldBodyUiModel | visibleEditInstanceSecrets = newVisibleSecrets }
          , Cmd.none
          )
      ToggleNewInstanceSecretVisibility templateId parameter ->
        let newVisibleSecrets =
          ( insertOrRemove
              ( not (Set.member (templateId, parameter) oldNewTemplateVisibleSecrets) )
              (templateId, parameter)
              oldNewTemplateVisibleSecrets
          )
        in
          ( { oldBodyUiModel | visibleNewTemplateSecrets = newVisibleSecrets }
          , Cmd.none
          )
      ExpandNewInstanceForm expanded templateId ->
        let newExpandedNewInstanceForms =
          if (expanded) then
            ( Dict.update
                templateId
                ( expandParameterForm templateId oldExpandedNewInstanceForms )
                oldExpandedNewInstanceForms
            )
            else
              Dict.remove templateId oldExpandedNewInstanceForms
        in
          ( { oldBodyUiModel | expandedNewInstanceForms = newExpandedNewInstanceForms }
          , Cmd.none
          )
      SubmitNewInstanceCreation templateId parameterValues ->
        let instanceCreationJson =
          ( InstanceCreation templateId parameterValues )
          |> InstanceCreation.encoder
        in
          ( oldBodyUiModel -- TODO reset the form when receiving a success message { oldBodyUiModel | expandedNewInstanceForms = resetNewParameterForm templateId oldExpandedNewInstanceForms }
          , CmdUtils.cmd (SendWsMsg instanceCreationJson CreateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      DiscardNewInstanceCreation templateId ->
        ( { oldBodyUiModel | expandedNewInstanceForms = resetNewParameterForm templateId oldExpandedNewInstanceForms }
        , Cmd.none
        )

expandParameterForm templateId oldExpandedNewInstanceForms maybeParameterForm =
  case maybeParameterForm of
    Just parameterForm -> Just parameterForm
    Nothing -> Just InstanceParameterForm.empty

resetEditParameterForm instance parameterForms =
  Dict.remove instance.id parameterForms

resetNewParameterForm templateId parameterForms =
  Dict.remove templateId parameterForms

updateEditInstanceParameterForm instance parameter value maybeParameterForm =
  case maybeParameterForm of
    Just parameterForm ->
      let oldParameterValues = parameterForm.changedParameterValues in
        Just
          ( { parameterForm | changedParameterValues = Dict.insert parameter value oldParameterValues } )
    Nothing ->
      Just
        ( { changedParameterValues = Dict.fromList [ ( parameter, value ) ]
          , originalParameterValues = instance.parameterValues
          }
        )

updateNewInstanceParameterForm parameter value maybeParameterForm =
  case maybeParameterForm of
    Just parameterForm ->
      let oldParameterValues = parameterForm.changedParameterValues in
        Just
          ( { parameterForm | changedParameterValues = Dict.insert parameter value oldParameterValues } )
    Nothing ->
      Just
        ( { changedParameterValues = Dict.fromList [ ( parameter, value ) ]
          , originalParameterValues = Dict.empty
          }
        )


insertOrRemove bool insert set =
  if (bool) then
      Set.insert insert set
    else
      Set.remove insert set

unionOrDiff bool set1 set2 =
  if (bool) then
      Set.union set1 set2
    else
      Set.diff set1 set2
