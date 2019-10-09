module Views.Body exposing (..)

import Array exposing (Array)
import Bootstrap.Grid as Grid
import Bootstrap.Grid.Col as Col
import Bootstrap.Grid.Row as Row
import Bootstrap.Utilities.Spacing as Spacing
import Dict exposing (Dict)
import Html exposing (..)
import Html.Attributes exposing (..)
import Messages exposing (..)
import Model exposing (TabState(..))
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.NodeResources exposing (NodeResources)
import Models.Resources.Role exposing (Role)
import Models.Resources.Template exposing (Template, TemplateId, addTemplateInstanceString)
import Models.Ui.BodyUiModel exposing (BodyUiModel, TemporaryStates)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Views.ResourcesView as ResourcesView
import Views.TemplateView as TemplateView


view :
    TabState
    -> Dict TemplateId Template
    -> Dict InstanceId Instance
    -> Dict InstanceId InstanceTasks
    -> List NodeResources
    -> BodyUiModel
    -> Maybe Role
    -> Html AnyMsg
view tabState templates instances tasks nodeResources bodyUiModel maybeRole =
    case tabState of
        Instances ->
            Html.map UpdateBodyViewMsg (instancesView templates instances tasks bodyUiModel maybeRole)

        Resources ->
            resourcesView bodyUiModel.temporaryStates nodeResources


instancesView : Dict TemplateId Template -> Dict InstanceId Instance -> Dict InstanceId InstanceTasks -> BodyUiModel -> Maybe Role -> Html UpdateBodyViewMsg
instancesView templates instances tasks bodyUiModel maybeRole =
    Grid.container
        [ Spacing.mt3, id "instances-view" ]
        (templates
            |> Dict.values
            |> List.map (TemplateView.view instances tasks templates bodyUiModel maybeRole)
        )


resourcesView : TemporaryStates -> List NodeResources -> Html AnyMsg
resourcesView temporaryStates nodesResources =
    Grid.container
        [ Spacing.mt5, id "resources-view" ]
        (List.concat
            [ ResourcesView.headerView
            , List.concat
                (nodesResources
                    |> List.map (ResourcesView.view temporaryStates)
                )
            ]
        )
