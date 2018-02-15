module Views.JobStatusView exposing (view)

import Models.Resources.JobStatus as JobStatus exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)


view : JobStatus -> Html msg
view jobStatus =
    let
        ( statusLabel, statusText ) =
            case jobStatus of
                JobRunning ->
                    ( "success", "running" )

                JobPending ->
                    ( "warning", "pending" )

                JobStopped ->
                    ( "default", "stopped" )

                JobDead ->
                    ( "primary", "completed" )

                JobUnknown ->
                    ( "warning", "unknown" )
    in
        span
            [ class (String.concat [ "hidden-xs label label-", statusLabel ])
            , style
                [ ( "font-size", "90%" )
                , ( "width", "80px" )
                , ( "display", "inline-block" )
                , ( "margin-right", "8px" )
                ]
            ]
            [ text statusText ]
