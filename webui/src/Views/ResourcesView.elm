module Views.ResourcesView exposing (view, headerView)

import Bootstrap.Grid as Grid
import Bootstrap.Grid.Col as Col
import Bootstrap.Grid.Row as Row
import Bootstrap.Text as Text
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Models.Resources.NodeResources exposing (..)
import Models.Ui.BodyUiModel exposing (ResourceType(..), TemporaryStates)
import Round
import Maybe.Extra exposing (isJust)
import Updates.Messages exposing (UpdateBodyViewMsg(UpdateTemporaryStates))
import Messages exposing (..)
import Utils.HtmlUtils exposing (iconButtonText)


headerView : List (Html AnyMsg)
headerView =
    [ Grid.row
        [ Row.attrs [ class "vertical-align" ] ]
        [ Grid.col
            [ Col.md2, Col.textAlign Text.alignMdCenter ]
            [ iconButtonText
                "btn btn-outline-secondary"
                "fa fa-refresh"
                "Refresh"
                [ onClick (SendWsMsg GetResources)
                , id "refresh-resources"
                ]
            ]
        , Grid.col
            [ Col.md8, Col.offsetMd2 ]
            [ Grid.row
                [ Row.attrs
                    [ class "border border-secondary vertical-align"
                    , style [ ( "height", "4rem" ), ( "background-color", "rgb(245,245,245)" ) ]
                    ]
                ]
                [ Grid.col
                    [ Col.md2, Col.textAlign Text.alignMdCenter ]
                    [ span
                        []
                        [ text "Legend" ]
                    ]
                , Grid.col
                    [ Col.md10, Col.textAlign Text.alignMdCenter ]
                    [ div
                        [ class "progress"
                        , style [ ( "height", "1.5rem" ), ( "width", "100%" ) ]
                        ]
                        [ div
                            [ class "progress-bar bg-warning"
                            , attribute "role" "progressbar"
                            , style [ ( "width", "33.33%" ) ]
                            , attribute "aria-valuenow" "33.33"
                            , attribute "aria-valuemin" "0"
                            , attribute "aria-valuemax" "100"
                            ]
                            [ text "User" ]
                        , div
                            [ class "progress-bar bg-info"
                            , attribute "role" "progressbar"
                            , style [ ( "width", "33.33%" ) ]
                            , attribute "aria-valuenow" "33.33"
                            , attribute "aria-valuemin" "0"
                            , attribute "aria-valuemax" "100"
                            ]
                            [ text "System (not used for memory)" ]
                        , div
                            [ class "progress-bar bg-success"
                            , attribute "role" "progressbar"
                            , style [ ( "width", "33.34%" ) ]
                            , attribute "aria-valuenow" "33.34"
                            , attribute "aria-valuemin" "0"
                            , attribute "aria-valuemax" "100"
                            ]
                            [ text "Idle/Free" ]
                        ]
                    ]
                ]
            ]
        ]
    , Html.map UpdateBodyViewMsg addHr
    , Grid.row
        []
        [ Grid.col
            [ Col.md2, Col.textAlign Text.alignMdCenter ]
            [ h6 [] [ text "Node Name" ] ]
        , Grid.col
            [ Col.md10, Col.textAlign Text.alignMdCenter ]
            [ Grid.row
                []
                [ Grid.col
                    [ Col.md2 ]
                    [ h6 [] [ text "Resource Type" ] ]
                , Grid.col
                    [ Col.md10 ]
                    [ Grid.row
                        []
                        [ Grid.col
                            [ Col.md2 ]
                            [ h6 [] [ text "Resource Name" ] ]
                        , Grid.col
                            [ Col.md10 ]
                            [ h6 [] [ text "Resource Value" ] ]
                        ]
                    ]
                ]
            ]
        ]
    , Html.map UpdateBodyViewMsg addHr
    ]


addHr =
    hr [] []


view : TemporaryStates -> NodeResources -> List (Html AnyMsg)
view temporaryStates nodeResources =
    [ Html.map
        UpdateBodyViewMsg
        (viewInternal temporaryStates nodeResources)
    , Html.map UpdateBodyViewMsg addHr
    ]


viewInternal : TemporaryStates -> NodeResources -> Html UpdateBodyViewMsg
viewInternal temporaryStates nodeResources =
    Grid.row
        [ Row.attrs [ class "vertical-align" ] ]
        [ Grid.col
            [ Col.md2, Col.textAlign Text.alignMdCenter ]
            [ text nodeResources.nodeName ]
        , Grid.col
            [ Col.md10, Col.textAlign Text.alignMdCenter ]
            [ Grid.row
                [ Row.attrs [ class "vertical-align" ] ]
                [ Grid.col
                    [ Col.md2 ]
                    [ text "Disk" ]
                , Grid.col
                    [ Col.md10 ]
                    (nodeResources.resources.disksStats
                        |> List.map (diskView temporaryStates nodeResources.nodeName)
                    )
                ]
            , addHr
            , Grid.row
                [ Row.attrs [ class "vertical-align" ] ]
                [ Grid.col
                    [ Col.md2 ]
                    [ text "Memory" ]
                , Grid.col
                    [ Col.md10 ]
                    [ memoryView temporaryStates nodeResources.nodeName nodeResources.resources.memoryStats ]
                ]
            , addHr
            , Grid.row
                [ Row.attrs [ class "vertical-align" ] ]
                [ Grid.col
                    [ Col.md2 ]
                    [ text "CPU" ]
                , Grid.col
                    [ Col.md10 ]
                    (nodeResources.resources.cpusStats
                        |> List.map (cpuView temporaryStates nodeResources.nodeName)
                    )
                ]
            ]
        ]


diskView : TemporaryStates -> String -> DiskInfo -> Html UpdateBodyViewMsg
diskView temporaryStates nodeName diskInfo =
    let
        used =
            (toFloat diskInfo.used)

        system =
            (toFloat (diskInfo.size - (diskInfo.available + diskInfo.used)))

        free =
            (toFloat diskInfo.available)

        usedPercent =
            diskInfo.usedPercent

        systemPercent =
            (system / (toFloat diskInfo.size)) * 100

        freePercent =
            100 - (systemPercent + usedPercent)

        -- 1073741824 = 1024 x 1024 x 1024
        usedString =
            String.concat [ Round.round 2 (used / 1073741824), "GB" ]

        systemString =
            String.concat [ Round.round 2 (system / 1073741824), "GB" ]

        freeString =
            String.concat [ Round.round 2 (free / 1073741824), "GB" ]
    in
        progressView
            temporaryStates
            nodeName
            Disk
            diskInfo.device
            used
            system
            free
            usedPercent
            systemPercent
            freePercent
            usedString
            systemString
            freeString


memoryView : TemporaryStates -> String -> MemoryInfo -> Html UpdateBodyViewMsg
memoryView temporaryStates nodeName memoryInfo =
    let
        used =
            (toFloat memoryInfo.used)

        system =
            0.0

        free =
            (toFloat memoryInfo.available)

        total =
            (toFloat memoryInfo.total)

        usedPercent =
            (used / total) * 100

        systemPercent =
            0.0

        freePercent =
            100 - usedPercent

        -- 1073741824 = 1024 x 1024 x 1024
        usedString =
            String.concat [ Round.round 2 (used / 1073741824), "GB" ]

        systemString =
            "0.00GB"

        freeString =
            String.concat [ Round.round 2 (free / 1073741824), "GB" ]
    in
        progressView
            temporaryStates
            nodeName
            Memory
            "Main Memory"
            used
            system
            free
            usedPercent
            systemPercent
            freePercent
            usedString
            systemString
            freeString


cpuView : TemporaryStates -> String -> CPUInfo -> Html UpdateBodyViewMsg
cpuView temporaryStates nodeName cpuInfo =
    let
        used =
            cpuInfo.user

        system =
            cpuInfo.system

        free =
            100 - (used + system)

        usedPercent =
            used

        systemPercent =
            system

        freePercent =
            free

        usedString =
            String.concat [ (Round.round 2 usedPercent), "%" ]

        systemString =
            String.concat [ (Round.round 2 systemPercent), "%" ]

        freeString =
            String.concat [ (Round.round 2 freePercent), "%" ]
    in
        progressView
            temporaryStates
            nodeName
            CPU
            cpuInfo.cpuName
            used
            system
            free
            usedPercent
            systemPercent
            freePercent
            usedString
            systemString
            freeString


progressView :
    TemporaryStates
    -> String
    -> ResourceType
    -> String
    -> Float
    -> Float
    -> Float
    -> Float
    -> Float
    -> Float
    -> String
    -> String
    -> String
    -> Html UpdateBodyViewMsg
progressView temporaryStates nodeName resourceType resourceName used system free usedPercent systemPercent freePercent usedString systemString freeString =
    let
        usedPosition =
            ((usedPercent / 2) - 2.5) * 0.9

        systemPosition =
            ((usedPercent + (systemPercent / 2)) - 2.5) * 0.9

        freePosition =
            ((usedPercent + systemPercent + (freePercent / 2)) - 2.5) * 0.9

        -- resolve tooltip
        ( hasHoverMessage, hoverMessage, hoverPosition ) =
            Maybe.withDefault
                ( False, "", 0 )
                (Maybe.map
                    (\rHMsg ->
                        ( rHMsg.nodeName
                            == nodeName
                            && rHMsg.resourceType
                            == resourceType
                            && rHMsg.resourceName
                            == resourceName
                        , rHMsg.message
                        , rHMsg.position
                        )
                    )
                    temporaryStates.resourceHoverMessage
                )
    in
        Grid.row
            [ Row.attrs [ class "vertical-align" ] ]
            [ Grid.col
                [ Col.md2 ]
                [ text resourceName ]
            , Grid.col
                [ Col.md10 ]
                [ div
                    [ class "progress m-3", style [ ( "height", "1.5rem" ) ] ]
                    (List.concat
                        [ [ progressBarView
                                temporaryStates
                                nodeName
                                resourceType
                                resourceName
                                used
                                usedPercent
                                usedString
                                usedPosition
                                "bg-warning"
                          , progressBarView
                                temporaryStates
                                nodeName
                                resourceType
                                resourceName
                                system
                                systemPercent
                                systemString
                                systemPosition
                                "bg-info"
                          , progressBarView
                                temporaryStates
                                nodeName
                                resourceType
                                resourceName
                                free
                                freePercent
                                freeString
                                freePosition
                                "bg-success"
                          ]
                        , if (hasHoverMessage) then
                            [ div
                                [ class "popover fade show bs-popover-top"
                                , style
                                    [ ( "left", String.concat [ toString hoverPosition, "%" ] )
                                    , ( "top", "-2rem" )
                                    , ( "display", "inline-block" )
                                    , ( "width", "6rem" )
                                    ]
                                ]
                                [ div
                                    [ class "arrow", style [ ( "left", "2rem" ) ] ]
                                    []
                                , div
                                    [ class "popover-body" ]
                                    [ text hoverMessage ]
                                ]
                            ]
                          else
                            []
                        ]
                    )
                ]
            ]


progressBarView :
    TemporaryStates
    -> String
    -> ResourceType
    -> String
    -> Float
    -> Float
    -> String
    -> Float
    -> String
    -> Html UpdateBodyViewMsg
progressBarView temporaryStates nodeName resourceType resourceName actual percent stringRep position progressBarClass =
    div
        [ class (String.concat [ "progress-bar ", progressBarClass ])
        , attribute "role" "progressbar"
        , style [ ( "width", String.concat [ toString percent, "%" ] ) ]
        , attribute "aria-valuenow" (Round.round 2 percent)
        , attribute "aria-valuemin" "0"
        , attribute "aria-valuemax" "100"
        , id (String.join "-" [ nodeName, resourceTypeToString resourceType, resourceName, progressBarClass ])
        , onMouseOver
            (UpdateTemporaryStates
                { temporaryStates
                    | resourceHoverMessage =
                        Just
                            { nodeName = nodeName
                            , resourceType = resourceType
                            , resourceName = resourceName
                            , message = stringRep
                            , position = position
                            }
                }
            )
        , onMouseOut
            (UpdateTemporaryStates
                { temporaryStates
                    | resourceHoverMessage = Nothing
                }
            )
        ]
        [ text
            (if (percent >= 10.0) then
                stringRep
             else
                ""
            )
        ]


resourceTypeToString : ResourceType -> String
resourceTypeToString resourceType =
    case resourceType of
        CPU ->
            "cpu"

        Disk ->
            "disk"

        Memory ->
            "memory"
