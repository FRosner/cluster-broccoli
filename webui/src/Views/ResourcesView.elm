module Views.ResourcesView exposing (headerView, view)

import Array exposing (Array)
import Bootstrap.Card as Card
import Bootstrap.Card.Block as Block
import Bootstrap.Grid as Grid exposing (Column)
import Bootstrap.Grid.Col as Col
import Bootstrap.Grid.Row as Row
import Bootstrap.Table as Table
import Bootstrap.Text as Text
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Maybe.Extra exposing (isJust)
import Messages exposing (..)
import Models.Resources.NodeResources exposing (..)
import Models.Ui.BodyUiModel exposing (ResourceHoverMessage, ResourceSubType(..), ResourceType(..), TemporaryStates)
import Round
import Set
import Updates.Messages exposing (UpdateBodyViewMsg(ToggleNodeAllocation, UpdateTemporaryStates))
import Utils.HtmlUtils exposing (icon, iconButtonText)


type alias ResourceValues =
    { used : Float
    , free : Float
    , usedString : String
    , freeString : String
    }


type RowType
    = Header
    | Even
    | Odd


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
        total =
            nodeResources.totalResources

        totalAllocated =
            nodeResources.totalAllocated

        totalUtilized =
            nodeResources.totalUtilized

        hostResources =
            nodeResources.hostResources

        cpuValues =
            { host =
                { used = hostResources.cpu |> toFloat
                , free = total.cpu - hostResources.cpu |> toFloat
                , usedString = makeCPUString hostResources.cpu
                , freeString = makeCPUString (total.cpu - hostResources.cpu)
                }
            , allocated =
                { used = totalAllocated.cpu |> toFloat
                , free = total.cpu - totalAllocated.cpu |> toFloat
                , usedString = makeCPUString totalAllocated.cpu
                , freeString = makeCPUString (total.cpu - totalAllocated.cpu)
                }
            , allocationUtilization =
                { used = totalUtilized.cpu |> toFloat
                , free = totalAllocated.cpu - totalUtilized.cpu |> toFloat
                , usedString = makeCPUString totalUtilized.cpu
                , freeString = makeCPUString (totalAllocated.cpu - totalUtilized.cpu)
                }
            }

        memoryValues =
            { host =
                { used = hostResources.memoryUsed |> toFloat
                , free = hostResources.memoryTotal - hostResources.memoryUsed |> toFloat
                , usedString = iBytes hostResources.memoryUsed
                , freeString = iBytes (hostResources.memoryTotal - hostResources.memoryUsed)
                }
            , allocated =
                { used = totalAllocated.memoryMB |> toFloat
                , free = total.memoryMB - totalAllocated.memoryMB |> toFloat
                , usedString = totalAllocated.memoryMB * bytesPerMegabyte |> iBytes
                , freeString = (total.memoryMB - totalAllocated.memoryMB) * bytesPerMegabyte |> iBytes
                }
            , allocationUtilization =
                { used = totalUtilized.memory |> toFloat
                , free = (totalAllocated.memoryMB * bytesPerMegabyte) - totalUtilized.memory |> toFloat
                , usedString = iBytes totalUtilized.memory
                , freeString = (totalAllocated.memoryMB * bytesPerMegabyte) - totalUtilized.memory |> iBytes
                }
            }

        diskValues =
            { host =
                { used = hostResources.diskUsed |> toFloat
                , free = hostResources.diskSize - hostResources.diskUsed |> toFloat
                , usedString = iBytes hostResources.diskUsed
                , freeString = iBytes (hostResources.diskSize - hostResources.diskUsed)
                }
            , allocated =
                { used = totalAllocated.diskMB |> toFloat
                , free = total.diskMB - totalAllocated.diskMB |> toFloat
                , usedString = totalAllocated.diskMB * bytesPerMegabyte |> iBytes
                , freeString = (total.diskMB - totalAllocated.diskMB) * bytesPerMegabyte |> iBytes
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
                            (allocatedResource.memoryMB * bytesPerMegabyte)
                                - allocatedResourceUtilization.memory
                                |> iBytes
                        }
                    }
                        :: allocationResourceConsumption
                )
                -- Do nothing if it exists only in the allocatedResourceUtilization Dict (Shouldn't happen normally)
                (\allocId allocatedResourceUtilization allocationResourceConsumption -> allocationResourceConsumption)
                -- The first dictionary to merge
                nodeResources.allocatedResources
                -- The second dictionary to merge
                nodeResources.allocatedResourcesUtilization
                -- Initial empty list for results
                []

        isExpanded =
            Set.member nodeResources.nodeId temporaryStates.expandedResourceAllocs
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
                    (List.append
                        [ Grid.row
                            [ Row.attrs [ class "mt-3 mb-3" ] ]
                            [ Grid.col
                                [ Col.md6 ]
                                (nodeResourceView
                                    temporaryStates
                                    nodeResources.nodeName
                                    CPU
                                    cpuValues.host
                                    cpuValues.allocated
                                    (Just cpuValues.allocationUtilization)
                                )
                            , Grid.col
                                [ Col.md6 ]
                                (nodeResourceView
                                    temporaryStates
                                    nodeResources.nodeName
                                    Memory
                                    memoryValues.host
                                    memoryValues.allocated
                                    (Just memoryValues.allocationUtilization)
                                )
                            ]
                        , Grid.row
                            [ Row.attrs [ class "mt-3 mb-3" ] ]
                            [ Grid.col
                                [ Col.md6 ]
                                (nodeResourceView
                                    temporaryStates
                                    nodeResources.nodeName
                                    Disk
                                    diskValues.host
                                    diskValues.allocated
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
                                    , h6 [ style [ ( "display", "inline" ) ] ] [ text "Allocations" ]
                                    ]
                                ]
                            ]
                        ]
                        (if isExpanded then
                            [ Grid.row
                                [ Row.attrs [ class "mt-2" ] ]
                                [ Grid.col
                                    []
                                    (List.concat
                                        [ [ fillAllocationRow
                                                (span [] [ text "Allocation Name" ])
                                                (span [] [ text "CPU" ])
                                                (span [] [ text "Memory" ])
                                                Header
                                          ]
                                        , List.indexedMap
                                            (\idx allocConsumption ->
                                                fillAllocationRow
                                                    (text allocConsumption.name)
                                                    (progressViewInternal
                                                        temporaryStates
                                                        nodeResources.nodeName
                                                        CPU
                                                        AllocatedUtilization
                                                        allocConsumption.name
                                                        allocConsumption.cpuValues
                                                    )
                                                    (progressViewInternal
                                                        temporaryStates
                                                        nodeResources.nodeName
                                                        Memory
                                                        AllocatedUtilization
                                                        allocConsumption.name
                                                        allocConsumption.memoryValues
                                                    )
                                                    (if idx % 2 == 0 then
                                                        Even

                                                     else
                                                        Odd
                                                    )
                                            )
                                            allocationsResourceConsumption
                                        ]
                                    )
                                ]
                            ]

                         else
                            []
                        )
                    )
            ]
        |> Card.view


fillAllocationRow : Html msg -> Html msg -> Html msg -> RowType -> Html msg
fillAllocationRow col1 col2 col3 rowType =
    Grid.row
        [ Row.attrs
            [ classList
                [ ( "text-center p-3 my-table", True )
                , ( "my-table-head", rowType == Header )
                , ( "my-odd-row", rowType == Odd )
                , ( "my-even-row", rowType == Even )
                ]
            ]
        ]
        [ Grid.col [ Col.md2 ] [ col1 ]
        , Grid.col [ Col.md5 ] [ col2 ]
        , Grid.col [ Col.md5 ] [ col3 ]
        ]


nodeResourceView :
    TemporaryStates
    -> String
    -> ResourceType
    -> ResourceValues
    -> ResourceValues
    -> Maybe ResourceValues
    -> List (Html UpdateBodyViewMsg)
nodeResourceView temporaryStates nodeName resourceType hostValues allocationValues maybeAllocationUtilizationValues =
    [ Grid.simpleRow
        [ Grid.col [] [ span [ class "mt-2" ] [ h6 [] [ text (resourceTypeToString resourceType) ] ] ] ]
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        (progressView temporaryStates nodeName resourceType Host "total" hostValues)
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        (progressView temporaryStates nodeName resourceType Allocated "total" allocationValues)
    , Grid.row [ Row.attrs [ class "vertical-align" ] ]
        (maybeAllocationUtilizationValues
            |> Maybe.map (\diskValues -> progressView temporaryStates nodeName resourceType AllocatedUtilization "total" diskValues)
            |> Maybe.withDefault [ Grid.col [] [] ]
        )
    ]


progressView :
    TemporaryStates
    -> String
    -> ResourceType
    -> ResourceSubType
    -> String
    -> ResourceValues
    -> List (Column UpdateBodyViewMsg)
progressView temporaryStates nodeName resourceType resourceSubType resourceId resourceValues =
    [ Grid.col [ Col.md3 ] [ span [ class "m2 sub-heading" ] [ text (resourceSubTypeToString resourceSubType) ] ]
    , Grid.col [ Col.md9 ]
        [ progressViewInternal temporaryStates nodeName resourceType resourceSubType resourceId resourceValues ]
    ]


progressViewInternal :
    TemporaryStates
    -> String
    -> ResourceType
    -> ResourceSubType
    -> String
    -> ResourceValues
    -> Html UpdateBodyViewMsg
progressViewInternal temporaryStates nodeName resourceType resourceSubType resourceId resourceValues =
    let
        ( usedPercent, freePercent ) =
            getPercents resourceValues.used (resourceValues.used + resourceValues.free)

        usedPosition =
            ((usedPercent / 2) - 5) * 0.9

        freePosition =
            ((usedPercent + (freePercent / 2)) - 5) * 0.9

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
            , if hasHoverMessage then
                [ div
                    [ class "popover fade show bs-popover-top"
                    , style
                        [ ( "left", String.concat [ toString hoverPosition, "%" ] )
                        , ( "top", "-2.5rem" )
                        , ( "display", "inline-block" )
                        ]
                    ]
                    [ div
                        [ class "arrow", style [ ( "left", "2rem" ) ] ]
                        []
                    , div
                        [ class "popover-body p-2", style [ ( "white-space", "nowrap" ) ] ]
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
            (UpdateTemporaryStates { temporaryStates | resourceHoverMessage = Just resourceHoverMessage })
        , onMouseOut
            (UpdateTemporaryStates
                { temporaryStates
                    | resourceHoverMessage = Nothing
                }
            )
        ]
        [ text
            (if percent >= 12.0 then
                stringRep

             else
                ""
            )
        ]


getPercents : Float -> Float -> ( Float, Float )
getPercents used total =
    let
        usedPercent =
            (used / total) * 100.0

        freePercent =
            100.0 - usedPercent
    in
    ( usedPercent, freePercent )


resourceTypeToString : ResourceType -> String
resourceTypeToString resourceSubType =
    case resourceSubType of
        CPU ->
            "CPU"

        Disk ->
            "Disk"

        Memory ->
            "Memory"


resourceSubTypeToString : ResourceSubType -> String
resourceSubTypeToString resourceType =
    case resourceType of
        Host ->
            "Host"

        Allocated ->
            "Allocated"

        AllocatedUtilization ->
            "Allocation Utilization"



-- Logic copied from Nomad source
-- https://github.com/hashicorp/nomad/blob/v0.5.4/vendor/github.com/dustin/go-humanize/bytes.go#L68


humanateBytes : Int -> Int -> Array String -> String
humanateBytes s base sizes =
    if s < 10 then
        toString s ++ " B"

    else
        let
            e =
                logBase (toFloat base) (toFloat s) |> floor

            suffix =
                Array.get e sizes |> Maybe.withDefault ""

            val =
                toFloat (toFloat s / toFloat (base ^ e) * 10 + 0.5 |> floor) / 10
        in
        Round.round 2 val ++ " " ++ suffix


iBytes : Int -> String
iBytes s =
    let
        sizes =
            Array.fromList [ "B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB" ]
    in
    humanateBytes s 1024 sizes


bytesPerMegabyte : Int
bytesPerMegabyte =
    1024 * 1024


makeCPUString a =
    toString a ++ " MHz"
