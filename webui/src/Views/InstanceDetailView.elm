module Views.InstanceDetailView exposing (view)

import Models.Resources.Role exposing (Role(..))
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Template exposing (Template)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Views.ParameterFormView as ParameterFormView
import Views.PeriodicRunsView as PeriodicRunsView
import Views.JobTasksView as JobTasksView
import Views.Styles as Styles
import Html exposing (..)
import Html.Attributes exposing (..)
import Dict exposing (..)
import Set exposing (Set)


view : Instance -> Maybe InstanceTasks -> Maybe InstanceParameterForm -> Set ( InstanceId, String ) -> Dict String Template -> Maybe Role -> Html UpdateBodyViewMsg
view instance instanceTasks maybeInstanceParameterForm visibleSecrets templates maybeRole =
    let
        periodicRuns =
            List.reverse (List.sortBy .utcSeconds instance.periodicRuns)
    in
        tr []
            [ td
                [ style Styles.expandedTdStyle
                , width Styles.checkboxColumnWidth
                ]
                []
            , td
                [ colspan 5
                , style
                    (List.append
                        Styles.expandedTdStyle
                        [ ( "padding-right", "40px" ) ]
                    )
                ]
                (List.concat
                    [ [ ParameterFormView.editView instance templates maybeInstanceParameterForm visibleSecrets maybeRole
                      ]
                    , (if (List.isEmpty periodicRuns) then
                        []
                       else
                        [ h6 [] [ text "Periodic Runs" ]
                        , PeriodicRunsView.view instance.id instanceTasks periodicRuns
                        ]
                      )
                    , JobTasksView.view instance.id (Maybe.map .allocatedTasks instanceTasks) True
                    ]
                )
            ]
