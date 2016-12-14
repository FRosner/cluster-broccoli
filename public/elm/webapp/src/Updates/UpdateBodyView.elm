module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)
import Messages exposing (AnyMsg)

updateBodyView : UpdateBodyViewMsg -> Set TemplateId -> Set InstanceId -> Set InstanceId -> (Set TemplateId, Set InstanceId, Set InstanceId, Cmd AnyMsg)
updateBodyView message oldExpandedTemplates oldSelectedInstances oldExpandedInstances =
  case message of
    ToggleTemplate templateId ->
      ( insertOrRemove (not (Set.member templateId oldExpandedTemplates)) templateId oldExpandedTemplates
      , oldSelectedInstances
      , oldExpandedInstances
      , Cmd.none
      )
    InstanceSelected instanceId selected ->
      ( oldExpandedTemplates
      , insertOrRemove selected instanceId oldSelectedInstances
      , oldExpandedInstances
      , Cmd.none
      )
    AllInstancesSelected instanceIds selected ->
      ( oldExpandedTemplates
      , unionOrDiff selected oldSelectedInstances (Set.fromList instanceIds)
      , oldExpandedInstances
      , Cmd.none
      )
    InstanceExpanded instanceId expanded ->
      ( oldExpandedTemplates
      , oldSelectedInstances
      , insertOrRemove expanded instanceId oldExpandedInstances
      , Cmd.none
      )
    AllInstancesExpanded instanceIds expanded ->
      ( oldExpandedTemplates
      , oldSelectedInstances
      , unionOrDiff expanded oldExpandedInstances (Set.fromList instanceIds)
      , Cmd.none
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
