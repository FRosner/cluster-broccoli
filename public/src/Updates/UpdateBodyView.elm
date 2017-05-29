module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))

import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Models.Resources.InstanceUpdate as InstanceUpdate exposing (InstanceUpdate)

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
          ( { oldBodyUiModel
            | expandedTemplates = newExpandedTemplates
            , attemptedDeleteInstances = Nothing
            }
          , Cmd.none
          )
      InstanceSelected instanceId selected ->
        let newSelectedInstances =
          insertOrRemove selected instanceId oldSelectedInstances
        in
          ( { oldBodyUiModel
            | selectedInstances = newSelectedInstances
            , attemptedDeleteInstances = Nothing
            }
          , Cmd.none
          )
      AllInstancesSelected instanceIds selected ->
        let newSelectedInstances =
          unionOrDiff selected oldSelectedInstances instanceIds
        in
          ( { oldBodyUiModel
            | selectedInstances = newSelectedInstances
            , attemptedDeleteInstances = Nothing
            }
          , Cmd.none
          )
      InstanceExpanded instanceId expanded ->
        let newExpandedInstances =
          insertOrRemove expanded instanceId oldExpandedInstances
        in
          ( { oldBodyUiModel
            | expandedInstances = newExpandedInstances
            , attemptedDeleteInstances = Nothing
            }
          , Cmd.none
          )
      AllInstancesExpanded instanceIds expanded ->
        let newExpandedInstances =
          unionOrDiff expanded oldExpandedInstances instanceIds
        in
          ( { oldBodyUiModel
            | expandedInstances = newExpandedInstances
            , attemptedDeleteInstances = Nothing
            }
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
          ( { oldBodyUiModel
            | instanceParameterForms = newInstanceParameterForms
            , attemptedDeleteInstances = Nothing
            }
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
          ( { oldBodyUiModel
            | instanceParameterForms = newInstanceParameterForms
            , attemptedDeleteInstances = Nothing
            }
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
          ( { oldBodyUiModel
            | expandedNewInstanceForms = newExpandedNewInstanceForms
            , attemptedDeleteInstances = Nothing
            }
          , Cmd.none
          )
      DiscardParameterValueChanges instanceId ->
        ( { oldBodyUiModel
          | instanceParameterForms = resetEditParameterForm instanceId oldInstanceParameterForms
          , attemptedDeleteInstances = Nothing
          }
        , Cmd.none
        )
      ApplyParameterValueChanges instance maybeInstanceParameterForm template ->
        let instanceUpdateJson =
          ( InstanceUpdate
              instance.id
              Nothing
              ( Maybe.map
                ( \f ->
                  f.originalParameterValues
                  |> Dict.union f.changedParameterValues
                  |> Dict.filter (\p v -> List.member p template.parameters)
                  |> Dict.map (\k v -> Maybe.withDefault "" v)
                )
                maybeInstanceParameterForm
              )
              ( Maybe.andThen (\f -> (Maybe.map (\t -> t.id)) f.selectedTemplate) maybeInstanceParameterForm )
          ) |> InstanceUpdate.encoder
        in
          ( { oldBodyUiModel
            | attemptedDeleteInstances = Nothing
            }
          , CmdUtils.sendMsg (SendWsMsg instanceUpdateJson UpdateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      ToggleEditInstanceSecretVisibility instanceId parameter ->
        let newVisibleSecrets =
          ( insertOrRemove
              ( not (Set.member (instanceId, parameter) oldEditInstanceVisibleSecrets) )
              (instanceId, parameter)
              oldEditInstanceVisibleSecrets
          )
        in
          ( { oldBodyUiModel
            | visibleEditInstanceSecrets = newVisibleSecrets
            , attemptedDeleteInstances = Nothing
            }
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
          ( { oldBodyUiModel
            | visibleNewTemplateSecrets = newVisibleSecrets
            , attemptedDeleteInstances = Nothing
            }
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
          ( { oldBodyUiModel
            | expandedNewInstanceForms = newExpandedNewInstanceForms
            , attemptedDeleteInstances = Nothing
            }
          , Cmd.none
          )
      SubmitNewInstanceCreation templateId parameterValues ->
        let instanceCreationJson =
          parameterValues
          |> Dict.map (\k v -> Maybe.withDefault "" v)
          |> InstanceCreation templateId
          |> InstanceCreation.encoder
        in
          -- TODO reset the form when receiving a success message { oldBodyUiModel | expandedNewInstanceForms = resetNewParameterForm templateId oldExpandedNewInstanceForms }
          ( { oldBodyUiModel
            | attemptedDeleteInstances = Nothing
            }
          , CmdUtils.sendMsg (SendWsMsg instanceCreationJson CreateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      StartInstance instanceId ->
        let instanceUpdateJson =
          ( InstanceUpdate instanceId (Just JobStatus.JobRunning) Nothing Nothing )
          |> InstanceUpdate.encoder
        in
          -- TODO reset the form when receiving a success message?
          ( { oldBodyUiModel
            | attemptedDeleteInstances = Nothing
            }
          , CmdUtils.sendMsg (SendWsMsg instanceUpdateJson UpdateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      StopInstance instanceId ->
        let instanceUpdateJson =
          ( InstanceUpdate instanceId (Just JobStatus.JobStopped) Nothing Nothing )
          |> InstanceUpdate.encoder
        in
          -- TODO reset the form when receiving a success message?
          ( { oldBodyUiModel
            | attemptedDeleteInstances = Nothing
            }
          , CmdUtils.sendMsg (SendWsMsg instanceUpdateJson UpdateInstanceMsgType) -- TODO avoid this by wrapping directly based on the message type
          )
      DiscardNewInstanceCreation templateId ->
        ( { oldBodyUiModel
          | expandedNewInstanceForms = resetNewParameterForm templateId oldExpandedNewInstanceForms
          , attemptedDeleteInstances = Nothing
          }
        , Cmd.none
        )
      DeleteSelectedInstances templateId selectedInstances ->
        ( { oldBodyUiModel
          | attemptedDeleteInstances = Nothing
          }
        , selectedInstances
          |> Set.toList
          |> List.map (\id -> CmdUtils.sendMsg (SendWsMsg (Encode.string id) DeleteInstanceMsgType))
          |> Cmd.batch
        )
      AttemptDeleteSelectedInstances templateId selectedInstances ->
        ( { oldBodyUiModel
          | attemptedDeleteInstances = Just (templateId, selectedInstances)
          }
        , Cmd.none
        )
      StartSelectedInstances selectedInstances ->
        ( { oldBodyUiModel
          | attemptedDeleteInstances = Nothing
          }
        , selectedInstances
          |> Set.toList
          |> List.map (\id -> CmdUtils.sendMsg (SendWsMsg (InstanceUpdate.encoder (InstanceUpdate id (Just JobStatus.JobRunning) Nothing Nothing)) UpdateInstanceMsgType))
          |> Cmd.batch
        )
      StopSelectedInstances selectedInstances ->
        ( { oldBodyUiModel
          | attemptedDeleteInstances = Nothing
          }
        , selectedInstances
          |> Set.toList
          |> List.map (\id -> CmdUtils.sendMsg (SendWsMsg (InstanceUpdate.encoder (InstanceUpdate id (Just JobStatus.JobStopped) Nothing Nothing)) UpdateInstanceMsgType))
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
          ( { parameterForm | changedParameterValues = Dict.insert parameter (Just value) oldParameterValues } )
    Nothing ->
      Just
        ( { changedParameterValues = Dict.fromList [ ( parameter, Just value ) ]
          , originalParameterValues = instance.parameterValues
          , selectedTemplate = Nothing
          }
        )

selectTemplate instance templates templateId maybeParameterForm =
  let selectedTemplate =
    if (templateId == "") then
      Nothing
    else
      Dict.get templateId templates
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
          ( { parameterForm | changedParameterValues = Dict.insert parameter (Just value) oldParameterValues } )
    Nothing ->
      Just
        ( { changedParameterValues = Dict.fromList [ ( parameter, Just value ) ]
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
