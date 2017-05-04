module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))

import Models.Resources.Template exposing (TemplateId, Template)
import Models.Resources.Instance exposing (InstanceId)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Models.Resources.InstanceUpdate as InstanceUpdate exposing (InstanceUpdate)

import Utils.MaybeUtils as MaybeUtils
import Utils.CmdUtils as CmdUtils

import Messages exposing (..)

import Dict exposing (Dict)

import Set exposing (Set)

import Json.Encode as Encode

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
              ( addParameterValue instance parameter value )
              oldInstanceParameterForms
          )
        in
          ( { oldBodyUiModel | instanceParameterForms = newInstanceParameterForms }
          , Cmd.none
          )
      SelectEditInstanceTemplate instance templates templateId ->
        let newInstanceParameterForms =
          ( Dict.update
              instance.id
              ( selectTemplate instance templates templateId )
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
      DiscardParameterValueChanges instanceId ->
        ( { oldBodyUiModel | instanceParameterForms = resetEditParameterForm instanceId oldInstanceParameterForms }
        , Cmd.none
        )
      ApplyParameterValueChanges instance maybeInstanceParameterForm ->
        let instanceUpdateJson =
          ( InstanceUpdate
              instance.id
              Nothing
              ( Maybe.map (\f -> (Dict.insert "id" instance.id f.changedParameterValues)) maybeInstanceParameterForm ) -- TODO why do we need to send the ID here?
              ( Maybe.andThen (\f -> (Maybe.map (\t -> t.id)) f.selectedTemplate) maybeInstanceParameterForm )
          ) |> InstanceUpdate.encoder
        in
          ( oldBodyUiModel
          , CmdUtils.cmd (SendWsMsg instanceUpdateJson UpdateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
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
      StartInstance instanceId ->
        let instanceUpdateJson =
          ( InstanceUpdate instanceId (Just JobStatus.JobRunning) Nothing Nothing )
          |> InstanceUpdate.encoder
        in
          ( oldBodyUiModel -- TODO reset the form when receiving a success message?
          , CmdUtils.cmd (SendWsMsg instanceUpdateJson UpdateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      StopInstance instanceId ->
        let instanceUpdateJson =
          ( InstanceUpdate instanceId (Just JobStatus.JobStopped) Nothing Nothing )
          |> InstanceUpdate.encoder
        in
          ( oldBodyUiModel -- TODO reset the form when receiving a success message?
          , CmdUtils.cmd (SendWsMsg instanceUpdateJson UpdateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      DiscardNewInstanceCreation templateId ->
        ( { oldBodyUiModel | expandedNewInstanceForms = resetNewParameterForm templateId oldExpandedNewInstanceForms }
        , Cmd.none
        )
      DeleteSelectedInstances selectedInstances ->
        ( oldBodyUiModel
        , selectedInstances
          |> Set.toList
          |> List.map (\id -> CmdUtils.cmd (SendWsMsg (Encode.string id) DeleteInstanceMsgType))
          |> Cmd.batch
        )

expandParameterForm templateId oldExpandedNewInstanceForms maybeParameterForm =
  case maybeParameterForm of
    Just parameterForm -> Just parameterForm
    Nothing -> Just InstanceParameterForm.empty

resetEditParameterForm instanceId parameterForms =
  Dict.remove instanceId parameterForms

resetNewParameterForm templateId parameterForms =
  Dict.remove templateId parameterForms

-- TODO edit also the template somewhere (probably another function) and keep in unchanged here
addParameterValue instance parameter value maybeParameterForm =
  case maybeParameterForm of
    Just parameterForm ->
      let oldParameterValues = parameterForm.changedParameterValues in
        Just
          ( { parameterForm | changedParameterValues = Dict.insert parameter value oldParameterValues } )
    Nothing ->
      Just
        ( { changedParameterValues = Dict.fromList [ ( parameter, value ) ]
          , originalParameterValues = instance.parameterValues
          , selectedTemplate = Nothing
          }
        )

selectTemplate instance templates templateId maybeParameterForm =
  let selectedTemplate =
    if (templateId == "") then
      Nothing
    else
      case (List.filter (\t -> t.id == templateId) templates) of -- TODO make templates a map so you can just look it up
        [t1] -> Just t1
        _ -> Nothing
  in
    case maybeParameterForm of
      Just parameterForm ->
        Just
          ( { parameterForm | selectedTemplate = selectedTemplate } )
      Nothing ->
        Just
          ( { changedParameterValues = Dict.empty
            , originalParameterValues = instance.parameterValues
            , selectedTemplate = selectedTemplate
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
          , selectedTemplate = Nothing
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
