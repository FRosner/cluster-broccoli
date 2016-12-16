module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Dict exposing (Dict)
import Messages exposing (AnyMsg)
import Utils.MaybeUtils as MaybeUtils

updateBodyView : UpdateBodyViewMsg -> BodyUiModel -> (BodyUiModel, Cmd AnyMsg)
updateBodyView message oldBodyUiModel =
  let
    ( oldExpandedTemplates
    , oldSelectedInstances
    , oldExpandedInstances
    , oldInstanceParameterForms
    ) =
    ( oldBodyUiModel.expandedTemplates
    , oldBodyUiModel.selectedInstances
    , oldBodyUiModel.expandedInstances
    , oldBodyUiModel.instanceParameterForms
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
      EnterParameterValue instance parameter value ->
        let newInstanceParameterForms =
          ( Dict.update
              instance.id
              ( updateParameterForm instance parameter value )
              oldInstanceParameterForms
          )
        in
          ( { oldBodyUiModel | instanceParameterForms = newInstanceParameterForms }
          , Cmd.none
          )

updateParameterForm instance parameter value maybeParameterForm =
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
