module Views.JobStatusView exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Models.Resources.JobStatus as JobStatus exposing (..)


view : String -> JobStatus -> Html msg
view classes jobStatus =
    let
        ( statusLabel, statusText ) =
            case jobStatus of
                JobRunning ->
                    ( "success", "running" )

                JobPending ->
                    ( "warning", "pending" )

                JobStopped ->
                    ( "secondary", "stopped" )

                JobDead ->
                    ( "primary", "completed" )

                JobUnknown ->
                    ( "warning", "unknown" )
    in
    span
        [ class (String.concat [ classes, " mr-1 pt-1 badge badge-", statusLabel ])
        , style
            [ ( "font-size", "90%" )
            , ( "width", "6rem" )
            , ( "display", "inline-block" )
            , ( "height", "1.5rem" )
            , ( "margin-right", "8px" )
            ]
        ]
        [ text statusText ]
