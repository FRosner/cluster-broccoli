module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)
import Messages exposing (AnyMsg)

updateBodyView : UpdateBodyViewMsg -> Set TemplateId -> Set InstanceId -> (Set TemplateId, Set InstanceId, Cmd AnyMsg)
updateBodyView message oldExpandedTemplates oldSelectedInstances =
  case message of
    ToggleTemplate templateId ->
      ( if (Set.member templateId oldExpandedTemplates) then
          Set.remove templateId oldExpandedTemplates
        else
          Set.insert templateId oldExpandedTemplates
      , oldSelectedInstances
      , Cmd.none
      )
    InstanceSelected instanceId selected ->
      ( oldExpandedTemplates
      , if (selected) then
          Set.insert instanceId oldSelectedInstances
        else
          Set.remove instanceId oldSelectedInstances
      , Cmd.none
      )
    AllInstancesSelected instanceIds selected ->
      let (instanceIdsSet) =
        Set.fromList instanceIds
      in
        ( oldExpandedTemplates
        , if (selected) then
            Set.union oldSelectedInstances instanceIdsSet
          else
            Set.diff oldSelectedInstances instanceIdsSet
        , Cmd.none
      )
