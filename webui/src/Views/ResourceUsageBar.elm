module Views.ResourceUsageBar exposing (cpuUsageBar, memoryUsageBar, unknown)

import Filesize
import Html exposing (..)
import Html.Attributes exposing (..)
import Round


unknown : Html msg
unknown =
    div
        [ class "progress"
        , style
            [ ( "width", "100px" )
            , ( "position", "relative" )
            , ( "margin-bottom", "0px" )
            ]
        , title "Unknown"
        ]
        [ div
            [ class "progress-bar"
            , attribute "role" "progressbar"
            , attribute "aria-valuemin" "0"
            , attribute "aria-valuenow" "0"
            , attribute "aria-valuemax" "100"
            , style
                [ ( "text-align", "center" )
                , ( "width", "0%" )
                ]
            ]
            []
        ]


cpuUsageBar : Float -> Float -> Html msg
cpuUsageBar current required =
    resourceUsageBar
        (Round.round 0 current ++ " MHz / " ++ Round.round 0 required ++ " MHz CPU used")
        current
        required


memoryUsageBar : Int -> Int -> Html msg
memoryUsageBar current required =
    resourceUsageBar
        (Filesize.format current ++ " of " ++ Filesize.format required ++ " memory used")
        (toFloat current)
        (toFloat required)


resourceUsageBar : String -> Float -> Float -> Html msg
resourceUsageBar tooltip current required =
    let
        ratio =
            current / required

        context =
            if ratio > 1.0 then
                "progress-bar-danger"

            else if ratio >= 0.8 then
                "progress-bar-warning"

            else
                "progress-bar-success"
    in
    div
        [ class "progress"
        , style
            [ ( "width", "100px" )
            , ( "position", "relative" )
            , ( "margin-bottom", "0px" )
            ]
        , title tooltip
        ]
        [ div
            [ class "progress-bar"
            , class context
            , attribute "role" "progressbar"
            , attribute "aria-valuemin" "0"
            , attribute "aria-valuenow" (Round.round 2 current)
            , attribute "aria-valuemax" (Round.round 2 current)
            , style
                [ ( "text-align", "center" )
                , ( "width", Round.round 0 (100 * Basics.min 1.0 ratio) ++ "%" )
                ]
            ]
            []
        , span
            [ style
                [ ( "position", "absolute" )
                , ( "left", "0" )
                , ( "width", "100%" )
                , ( "text-align", "center" )
                , ( "z-index", "2" )
                ]
            ]
            [ text (Round.round 0 (ratio * 100)), text "%" ]
        ]
