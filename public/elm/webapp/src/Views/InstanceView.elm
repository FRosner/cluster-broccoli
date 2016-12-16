module Views.InstanceView exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.ServiceStatus exposing (..)
import Models.Resources.JobStatus exposing (..)
import Models.Resources.Template exposing (..)
import Set exposing (Set)
import Maybe
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Utils.MaybeUtils as MaybeUtils

checkboxColumnWidth = 1
chevronColumnWidth = 30
templateVersionColumnWidth = 1
jobControlsColumnWidth = 170

view services instances jobStatuses selectedInstances expandedInstances =
  let (instancesIds) =
    instances
      |> List.map (\i -> i.id)
      |> Set.fromList
  in
    let (allInstancesSelected, allInstancesExpanded) =
      ( ( instancesIds
          |> Set.intersect selectedInstances
          |> (==) instancesIds
        )
      , (Set.intersect instancesIds expandedInstances) == instancesIds
      )
    in
      table
        [ class "table"
        , style [ ("margin-bottom", "0px") ]
        ]
        [ thead []
          [ tr []
            [ th
              [ width checkboxColumnWidth ]
              [ input
                [ type_ "checkbox"
                , title "Select All"
                , onCheck (AllInstancesSelected (List.map (\i -> i.id) instances))
                , checked allInstancesSelected
                ]
                []
              ]
            , th
              [ width chevronColumnWidth ]
              [ icon
                ( String.concat
                  [ "fa fa-chevron-"
                  , if (allInstancesExpanded) then "down" else "right"
                  ]
                )
                [ attribute "role" "button"
                , onClick
                    ( AllInstancesExpanded
                      (List.map (\i -> i.id) instances)
                      (not allInstancesExpanded)
                    )
                ]
              ]
            , th []
              [ icon "fa fa-hashtag" [ title "Instance ID" ] ]
            , th [ class "text-left hidden-xs" ]
              [ icon "fa fa-cubes" [ title "Services" ] ]
            , th
              [ class "text-center hidden-xs"
              , width templateVersionColumnWidth
              ]
              [ icon "fa fa-code-fork" [ title "Template Version" ] ]
            , th
              [ class "text-center"
              , width jobControlsColumnWidth
              ]
              [ icon "fa fa-cogs" [ title "Job Controls" ] ]
            ]
          ]
        , tbody []
          ( List.concatMap (instanceRow services jobStatuses selectedInstances expandedInstances) instances )
        ]

instanceRow services jobStatuses selectedInstances expandedInstances instance =
  let
    (maybeInstanceServices, jobStatus, instanceExpanded) =
    ( Dict.get instance.id services
    , Maybe.withDefault JobUnknown (Dict.get instance.id jobStatuses)
    , (Set.member instance.id expandedInstances)
    )
  in
    List.append
    [ tr []
      [ td
        [ width checkboxColumnWidth ]
        [ input
          [ type_ "checkbox"
          , onCheck (InstanceSelected instance.id)
          , checked (Set.member instance.id selectedInstances)
          ]
          []
        ]
      , td
        [ width chevronColumnWidth ]
        [ icon
          ( String.concat
            [ "fa fa-chevron-"
            , if (Set.member instance.id expandedInstances) then "down" else "right"
            ]
          )
          [ attribute "role" "button"
          , onClick (InstanceExpanded instance.id (not instanceExpanded))
          ]
        ]
      , td []
        [ span
            [ attribute "role" "button"
            , onClick (InstanceExpanded instance.id (not instanceExpanded))
            ]
            [ text instance.id ]
        ]
      , td [ class "text-left hidden-xs" ]
        ( servicesView maybeInstanceServices )
      , td
        [ class "text-center hidden-xs"
        , width templateVersionColumnWidth
        ]
        [ span
          [ style [ ("font-family", "monospace") ] ]
          [ text (String.left 8 instance.template.version) ]
        ]
      , td
        [ class "text-center"
        , width jobControlsColumnWidth
        ]
        [ jobStatusView jobStatus
        , text " "
        , iconButton "btn btn-default btn-xs" "glyphicon glyphicon-play" "Start Instance"
        , text " "
        , iconButton "btn btn-default btn-xs" "glyphicon glyphicon-stop" "Stop Instance"
        ]
      ]
    ]
    ( if (instanceExpanded) then
        [ instanceDetailView
            instance
        ]
      else
        []
    )

expandedTdStyle =
  [ ("border-top", "0px")
  , ("padding-top", "0px")
  ]

editingParamColor = "rgba(255, 177, 0, 0.46)"
normalParamColor = "#eee"

-- TODO as "id" is special we should treat it also special
instanceDetailView instance =
  let
    ( ( idParameter
      , idParameterValue
      , idParameterInfo
      )
    , ( otherParameters
      , otherParameterValues
      , otherParameterInfos
      )
    ) =
    ( ( "id"
      , Dict.get "id" instance.parameterValues
      , Dict.get "id" instance.template.parameterInfos
      )
    , ( List.filter (\p -> p /= "id") instance.template.parameters
      , Dict.remove "id" instance.parameterValues
      , Dict.remove "id" instance.template.parameterInfos
      )
    )
  in
    let (otherParametersLeft, otherParametersRight) =
      let firstHalf =
        otherParameters
          |> List.length
          |> toFloat
          |> (\l -> l / 2)
          |> ceiling
      in
        ( List.take firstHalf otherParameters
        , List.drop firstHalf otherParameters
        )
    in
      tr []
        [ td
          [ style expandedTdStyle
          , width checkboxColumnWidth
          ]
          []
        , td
          [ colspan 5
          , style
            ( List.append
                expandedTdStyle
                [ ("padding-right", "40px") ]
            )
          ]
          [ h5 [] [ text "Template" ]
          , h5 [] [ text "Parameters" ]
          , div
            [ class "row" ]
            [ div
              [ class "col-md-6" ]
              [ ( parameterValueView (idParameter, idParameterValue, idParameterInfo, False) ) ]
            ]
          , div
            [ class "row" ]
            [ div
              [ class "col-md-6" ]
              ( parameterValuesView otherParametersLeft otherParameterValues otherParameterInfos )
            , div
              [ class "col-md-6" ]
              ( parameterValuesView otherParametersRight otherParameterValues otherParameterInfos )
            ]
          , div
            [ class "row"
            , style [ ("margin-bottom", "15px") ]
            ]
            [ div
              [ class "col-md-6" ]
              [ iconButtonText "btn btn-default" "fa fa-check" "Apply" []
              , text " "
              , iconButtonText "btn btn-default" "fa fa-ban" "Discard" []
              ]
            ]
          , h5 [] [ text "Periodic Runs" ]
          ]
        ]

parameterValuesView parameters parameterValues parameterInfos =
  parameters
  |> List.map ( \p -> (p, Dict.get p parameterValues, Dict.get p parameterInfos, True) )
  |> List.map parameterValueView

parameterValueView (parameter, maybeParameterValue, maybeParameterInfo, enabled) =
  let
    ( placeholderValue
    , parameterValue
    ) =
    ( maybeParameterInfo
        |> MaybeUtils.concatMap (\i -> i.default)
        |> Maybe.withDefault ""
    , maybeParameterValue
        |> Maybe.withDefault ""
    )
  in
    p
      []
      [ div
        [ class "input-group" ]
        [ span
          [ class "input-group-addon" ]
          [ text parameter ]
        , input
          [ type_ "text"
          , class "form-control"
          , attribute "aria-label" parameter
          , placeholder placeholderValue
          , value parameterValue
          , disabled (not enabled)
          ]
          []
        ]
      ]

jobStatusView jobStatus =
  let (statusLabel, statusText) =
    case jobStatus of
      JobRunning -> ("success", "running")
      JobPending -> ("warning", "pending")
      JobStopped -> ("default", "stopped")
      JobDead    -> ("primary", "completed")
      JobUnknown -> ("warning", "unknown")
  in
    span
      [ class ( String.concat [ "label label-", statusLabel ] )
      , style
        [ ("font-size", "90%")
        , ("width", "80px")
        , ("display", "inline-block")
        , ("margin-right", "8px")
        ]
      ]
      [ text statusText ]

servicesView maybeServices =
  case maybeServices of
    Just services ->
      (List.concatMap serviceView services)
    Nothing ->
      [ text "-" ]

serviceView service =
  let (iconClass, textColor) =
    case service.status of
      ServicePassing ->
        ("fa fa-check-circle", "#070")
      ServiceFailing ->
        ("fa fa-times-circle", "#900")
      ServiceUnknown ->
        ("fa fa-question-circle", "grey")
  in
    [ a
      [ href
        ( String.concat
          [ service.protocol
          , "://"
          , service.address
          , ":"
          , (toString service.port_)
          ]
        )
      , style
        [ ("margin-right", "8px")
        , ("color", textColor)
        ]
      ]
      [ icon iconClass [ style [ ("margin-right", "4px") ] ]
      , text service.name
      ]
    , text " "
    ]
