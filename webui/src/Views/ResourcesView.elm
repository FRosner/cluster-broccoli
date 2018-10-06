module Views.ResourcesView exposing (view, headerView)

import Array exposing (Array)
import Bootstrap.Grid as Grid exposing (Column)
import Bootstrap.Grid.Col as Col
import Bootstrap.Grid.Row as Row
import Bootstrap.Card as Card
import Bootstrap.Card.Block as Block
import Bootstrap.Table as Table
import Bootstrap.Text as Text
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Models.Resources.NodeResources exposing (..)
import Models.Ui.BodyUiModel exposing (ResourceHoverMessage, ResourceSubType(CPU, Disk, Memory), ResourceType(..), TemporaryStates)
import Round
import Maybe.Extra exposing (isJust)
import Set
import Updates.Messages exposing (UpdateBodyViewMsg(ToggleNodeAllocation, UpdateTemporaryStates))
import Messages exposing (..)
import Utils.HtmlUtils exposing (icon, iconButtonText)


type alias ResourceValues =
    { used : Float
    , free : Float
    , usedString: String
    , freeString : String
    }


headerView : List (Html AnyMsg)
headerView =
    [ Grid.row
        [ Row.attrs [ class "vertical-align" ] ]
        [ Grid.col
            [ Col.md2 ]
            [ iconButtonText
                "btn btn-outline-secondary"
                "fa fa-refresh"
                "Refresh"
                [ onClick (SendWsMsg GetResources)
                , id "refresh-resources"
                ]
            ]
        ]
    ]


addHr =
    hr [] []


view : TemporaryStates -> NodeResources -> List (Html AnyMsg)
view temporaryStates nodeResources =
    [ Html.map
        UpdateBodyViewMsg
        (viewInternal temporaryStates nodeResources)
    ]


viewInternal : TemporaryStates -> NodeResources -> Html UpdateBodyViewMsg
viewInternal temporaryStates nodeResources =
    let
        total = nodeResources.totalResources
        totalAllocated = nodeResources.totalAllocated
        totalUtilized = nodeResources.totalUtilized
        hostResources = nodeResources.hostResources

        hostResourcesValues =
            { cpuValues =
                { used = hostResources.cpu |> toFloat
                , free = total.cpu - hostResources.cpu |> toFloat
                , usedString = makeCPUString hostResources.cpu
                , freeString = makeCPUString (total.cpu - hostResources.cpu)
                }
            , memoryValues =
                { used = hostResources.memoryUsed |> toFloat
                , free = hostResources.memoryTotal - hostResources.memoryUsed |> toFloat
                , usedString = iBytes hostResources.memoryUsed
                , freeString = iBytes (hostResources.memoryTotal - hostResources.memoryUsed)
                }
            , diskValues =
                { used = hostResources.diskUsed |> toFloat
                , free = hostResources.diskSize - hostResources.diskUsed |> toFloat
                , usedString = iBytes hostResources.diskUsed
                , freeString = iBytes (hostResources.diskSize - hostResources.diskUsed)
                }
            }

        allocatedResourcesValues =
            { cpuValues =
                { used = totalAllocated.cpu |> toFloat
                , free = total.cpu - totalAllocated.cpu |> toFloat
                , usedString = makeCPUString totalAllocated.cpu
                , freeString = makeCPUString (total.cpu - totalAllocated.cpu)
                }
            , memoryValues =
                { used = totalAllocated.memoryMB |> toFloat
                , free = total.memoryMB - totalAllocated.memoryMB |> toFloat
                , usedString = totalAllocated.memoryMB * bytesPerMegabyte |> iBytes
                , freeString = (total.memoryMB - totalAllocated.memoryMB) * bytesPerMegabyte |> iBytes
                }
            , diskValues =
                { used = totalAllocated.diskMB |> toFloat
                , free = total.diskMB - totalAllocated.diskMB |> toFloat
                , usedString = totalAllocated.diskMB * bytesPerMegabyte |> iBytes
                , freeString = (total.diskMB - totalAllocated.diskMB) * bytesPerMegabyte |> iBytes
                }
            }

        allocatedResourcesUtilizationValues =
            { cpuValues =
                { used = totalUtilized.cpu |> toFloat
                , free = totalAllocated.cpu - totalUtilized.cpu |> toFloat
                , usedString = makeCPUString totalUtilized.cpu
                , freeString = makeCPUString (totalAllocated.cpu - totalUtilized.cpu)
                }
            , memoryValues =
                { used = totalUtilized.memory |> toFloat
                , free = (totalAllocated.memoryMB * bytesPerMegabyte) - totalUtilized.memory |> toFloat
                , usedString = iBytes totalUtilized.memory
                , freeString = (totalAllocated.memoryMB * bytesPerMegabyte) - totalUtilized.memory |> iBytes
                }
            }

        allocationsResourceConsumption =
            Dict.merge
                -- Do nothing if it exists only in the allocatedResource Dict (Shouldn't happen normally)
                (\allocId allocatedResource allocationResourceConsumption -> allocationResourceConsumption)
                -- Combine results if present in both
                (\allocId allocatedResource allocatedResourceUtilization allocationResourceConsumption ->
                    { id = allocId
                    , name = allocatedResource.name
                    , cpuValues =
                        { used = allocatedResourceUtilization.cpu |> toFloat
                        , free = allocatedResource.cpu - allocatedResourceUtilization.cpu |> toFloat
                        , usedString = makeCPUString allocatedResourceUtilization.cpu
                        , freeString = makeCPUString (allocatedResource.cpu - allocatedResourceUtilization.cpu)
                        }
                    , memoryValues =
                        { used = allocatedResourceUtilization.memory |> toFloat
                        , free = (allocatedResource.memoryMB * bytesPerMegabyte) - allocatedResourceUtilization.memory |> toFloat
                        , usedString = iBytes allocatedResourceUtilization.memory
                        , freeString =
                            (allocatedResource.memoryMB * bytesPerMegabyte) - allocatedResourceUtilization.memory
                                |> iBytes
                        }
                    } :: allocationResourceConsumption
                )
                -- Do nothing if it exists only in the allocatedResourceUtilization Dict (Shouldn't happen normally)
                (\allocId allocatedResourceUtilization allocationResourceConsumption -> allocationResourceConsumption)
                -- The first dictionary to merge
                nodeResources.allocatedResources
                -- The second dictionary to merge
                nodeResources.allocatedResourcesUtilization
                -- Initial empty list for results
                []

        isExpanded = Set.member nodeResources.nodeId temporaryStates.expandedResourceAllocs
    in
        Card.config [ Card.attrs [ class "mt-3" ] ]
            |> Card.header
                [ id nodeResources.nodeId ]
                [ span
                    [ style [ ( "font-size", "125%" ), ( "margin-right", "10px" ) ] ]
                    [ text nodeResources.nodeName ]
                ]
            |> Card.block []
                [ Block.custom <|
                    Grid.container
                        []
                        ( List.append
                            [ Grid.row
                                [ Row.attrs [ class "mt-3 mb-3" ] ]
                                [ Grid.col
                                    [ Col.md6 ]
                                    (nodeResourceView
                                        temporaryStates nodeResources.nodeName Allocated
                                        allocatedResourcesValues.cpuValues
                                        allocatedResourcesValues.memoryValues
                                        (Just allocatedResourcesValues.diskValues)
                                    )
                                , Grid.col
                                    [ Col.md6 ]
                                    (nodeResourceView
                                        temporaryStates nodeResources.nodeName Host
                                        hostResourcesValues.cpuValues
                                        hostResourcesValues.memoryValues
                                        (Just hostResourcesValues.diskValues)
                                    )
                                ]
                            , Grid.row
                                [ Row.attrs [ class "mt-3 mb-3" ] ]
                                [ Grid.col
                                    [ Col.md6 ]
                                    (nodeResourceView
                                        temporaryStates nodeResources.nodeName AllocatedUtilization
                                        allocatedResourcesUtilizationValues.cpuValues
                                        allocatedResourcesUtilizationValues.memoryValues
                                        Nothing
                                    )
                                ]
                            , Grid.row [ Row.attrs [ class "mt-4" ] ]
                                [ Grid.col []
                                    [ a
                                        [ id (String.concat [ "expand-allocations-", nodeResources.nodeId ])
                                        , class "btn btn-no-pad"
                                        , attribute "role" "button"
                                        , onClick (ToggleNodeAllocation nodeResources.nodeId)
                                        ]
                                        [ icon
                                            (String.concat
                                                [ "fa fa-chevron-"
                                                , if isExpanded then
                                                    "down"
                                                  else
                                                    "right"
                                                ]
                                            )
                                            [ style [ ( "margin-right", "4px" ) ] ]
                                        , h6 [ style [ ("display", "inline") ] ] [ text "Allocations" ]
                                        ]
                                    ]
                                ]
                            ]
                            ( if isExpanded then
                                [ Grid.row
                                    [ Row.attrs [ class "mt-2" ] ]
                                    [ Grid.col
                                        []
                                        [ Table.table
                                            { options = [ Table.striped, Table.hover ]
                                            , thead =
                                                Table.thead
                                                    [ Table.headAttr (class ".th-no-bold") ]
                                                    [Table.tr
                                                        []
                                                        [ Table.th [ Table.cellAttr (attribute "width"  "20%") ] [ text "Allocation Name" ]
                                                        , Table.th [ Table.cellAttr (attribute "width"  "30%") ] [ text "CPU" ]
                                                        , Table.th [ Table.cellAttr (attribute "width"  "30%") ] [ text "Memory" ]
                                                        ]
                                                    ]
                                            , tbody =
                                                Table.tbody []
                                                    (List.map
                                                        (\allocConsumption ->
                                                            Table.tr []
                                                                [ Table.td [] [ text allocConsumption.name ]
                                                                , Table.td []
                                                                    [ progressViewInternal
                                                                        temporaryStates nodeResources.nodeName
                                                                        AllocatedUtilization CPU allocConsumption.name
                                                                        allocConsumption.cpuValues
                                                                    ]
                                                                , Table.td []
                                                                    [ progressViewInternal
                                                                        temporaryStates nodeResources.nodeName
                                                                        AllocatedUtilization Memory allocConsumption.name
                                                                        allocConsumption.memoryValues
                                                                    ]
                                                                ]
                                                        )
                                                        allocationsResourceConsumption
                                                    )
                                            }
                                        ]
                                    ]
                                ]
                              else []
                            )
                        )
                ]
            |> Card.view


nodeResourceView : TemporaryStates -> String -> ResourceType -> ResourceValues -> ResourceValues -> Maybe ResourceValues ->
    List (Html UpdateBodyViewMsg)
nodeResourceView temporaryStates nodeName resourceType cpuValues memoryValues maybeDiskValues =
    [ Grid.simpleRow
        [ Grid.col [] [ span [ class "mt-2" ] [ h6 [] [ text (resourceTypeToString resourceType) ] ] ] ]
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        (progressView temporaryStates nodeName resourceType CPU "total" cpuValues)
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        (progressView temporaryStates nodeName resourceType Memory "total" memoryValues)
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        (maybeDiskValues
            |> Maybe.map (\diskValues -> progressView temporaryStates nodeName resourceType Disk "total" diskValues)
            |> Maybe.withDefault [ Grid.col [] [] ]
        )
    ]


allocationResourceView : TemporaryStates -> String -> String -> ResourceValues -> ResourceValues -> List (Html UpdateBodyViewMsg)
allocationResourceView temporaryStates nodeName allocName cpuValues memoryValues =
    [ Grid.simpleRow
        [ Grid.col [] [ span [ class "mt-2" ] [ h6 [] [ text "Allocations" ] ] ] ]
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        [ Grid.col [ Col.md6 ]
            [ Grid.simpleRow
                (progressView temporaryStates nodeName AllocatedUtilization CPU allocName cpuValues)
            ]
        , Grid.col [ Col.md6 ]
            [ Grid.simpleRow
                (progressView temporaryStates nodeName AllocatedUtilization Memory allocName memoryValues)
            ]
        ]
    ]


progressView : TemporaryStates -> String -> ResourceType -> ResourceSubType -> String -> ResourceValues ->
    List (Column UpdateBodyViewMsg)
progressView temporaryStates nodeName resourceType resourceSubType resourceId resourceValues =
    [ Grid.col [ Col.md2 ] [ span [ class "m2" ] [ text (resourceSubTypeToString resourceSubType) ] ]
    , Grid.col [ Col.offsetMd1, Col.md9 ]
        [ progressViewInternal temporaryStates nodeName resourceType resourceSubType resourceId resourceValues ]
    ]


progressViewInternal : TemporaryStates -> String -> ResourceType -> ResourceSubType -> String -> ResourceValues ->
    Html UpdateBodyViewMsg
progressViewInternal temporaryStates nodeName resourceType resourceSubType resourceId resourceValues =
    let
        (usedPercent, freePercent) = getPercents resourceValues.used (resourceValues.used + resourceValues.free)

        usedPosition =
            ((usedPercent / 2) - 5) * 0.9
        freePosition =
            ((usedPercent + (freePercent / 2)) - 5) * 0.9

        -- resolve tooltip
        ( hasHoverMessage, hoverMessage, hoverPosition ) =
            Maybe.withDefault
                ( False, "", 0 )
                ( Maybe.map
                    (\rHMsg ->
                        ( rHMsg.nodeName
                            == nodeName
                            && rHMsg.resourceType
                            == resourceType
                            && rHMsg.resourceSubType
                            == resourceSubType
                            && rHMsg.resourceId
                            == resourceId
                        , rHMsg.message
                        , rHMsg.position
                        )
                    )
                    temporaryStates.resourceHoverMessage
                )
    in
        div
            [ class "progress m-2", style [ ( "height", "1.2rem" ) ] ]
            (List.concat
                [ [ progressBarView
                        temporaryStates
                        { nodeName = nodeName
                        , resourceType = resourceType
                        , resourceSubType = resourceSubType
                        , resourceId = resourceId
                        , message = resourceValues.usedString
                        , position = usedPosition
                        }
                        resourceValues.used
                        usedPercent
                        resourceValues.usedString
                        "bg-warning"
                  , progressBarView
                        temporaryStates
                        { nodeName = nodeName
                        , resourceType = resourceType
                        , resourceSubType = resourceSubType
                        , resourceId = resourceId
                        , message = resourceValues.freeString
                        , position = freePosition
                        }
                        resourceValues.free
                        freePercent
                        resourceValues.freeString
                        "bg-success"
                  ]
                , if (hasHoverMessage) then
                    [ div
                        [ class "popover fade show bs-popover-top"
                        , style
                            [ ( "left", String.concat [ toString hoverPosition, "%" ] )
                            , ( "top", "-2.5rem" )
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


progressBarView :
    TemporaryStates
    -> ResourceHoverMessage
    -> Float
    -> Float
    -> String
    -> String
    -> Html UpdateBodyViewMsg
progressBarView temporaryStates resourceHoverMessage actual percent stringRep progressBarClass =
    div
        [ class (String.concat [ "progress-bar ", progressBarClass ])
        , attribute "role" "progressbar"
        , style [ ( "width", String.concat [ toString percent, "%" ] ) ]
        , attribute "aria-valuenow" (Round.round 2 percent)
        , attribute "aria-valuemin" "0"
        , attribute "aria-valuemax" "100"
        , id
            (String.join
                "-"
                [ resourceHoverMessage.nodeName
                , resourceTypeToString resourceHoverMessage.resourceType
                , resourceSubTypeToString resourceHoverMessage.resourceSubType
                , resourceHoverMessage.resourceId
                , progressBarClass
                ]
            )
        , onMouseOver
            (UpdateTemporaryStates { temporaryStates | resourceHoverMessage = Just resourceHoverMessage } )
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


getPercents : Float -> Float -> (Float, Float)
getPercents used total =
    let
        usedPercent = (used / total) * 100.0
        freePercent = 100.0 - usedPercent
    in
        (usedPercent, freePercent)


resourceSubTypeToString : ResourceSubType -> String
resourceSubTypeToString resourceSubType =
    case resourceSubType of
        CPU ->
            "CPU"

        Disk ->
            "Disk"

        Memory ->
            "Memory"


resourceTypeToString : ResourceType -> String
resourceTypeToString resourceType =
    case resourceType of
        Host ->
            "Host Resource Utilization"

        Allocated ->
            "Allocated Resources"

        AllocatedUtilization ->
            "Allocation Resource Utilization"


-- Logic copied from Nomad source
-- https://github.com/hashicorp/nomad/blob/v0.5.4/vendor/github.com/dustin/go-humanize/bytes.go#L68
humanateBytes : Int -> Int -> Array String -> String
humanateBytes s base sizes =
    if s < 10 then
        (toString s) ++ " B"
    else
        let
            e = logBase (toFloat base) (toFloat s) |> floor
            suffix = Array.get e sizes |> Maybe.withDefault ""
            val = toFloat ((toFloat s) / ( toFloat (base^e)) * 10 + 0.5 |> floor) / 10
        in
            (Round.round 2 val) ++ " " ++ suffix


iBytes : Int -> String
iBytes s =
    let
        sizes = Array.fromList ["B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB"]
    in
        humanateBytes s 1024 sizes

bytesPerMegabyte : Int
bytesPerMegabyte = 1024 * 1024

makeCPUString a = (toString a) ++ " MHz"