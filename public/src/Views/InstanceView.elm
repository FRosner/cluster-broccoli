module Views.InstanceView exposing (view)

import Models.Resources.ServiceStatus exposing (..)
import Models.Resources.JobStatus as JobStatus exposing (..)

import Updates.Messages exposing (UpdateBodyViewMsg(..))

import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)

import Views.ParameterFormView as ParameterFormView

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit)

import Dict exposing (..)

import Set exposing (Set)

import Date

checkboxColumnWidth = 1
chevronColumnWidth = 30
-- nameColumnWidth = 200
serviceColumnWidth = 500
templateVersionColumnWidth = 1
jobControlsColumnWidth = 200

view instances selectedInstances expandedInstances instanceParameterForms visibleSecrets templates =
  let (instancesIds) =
    instances
      |> Dict.keys
      |> Set.fromList
  in
    let (allInstancesSelected, allInstancesExpanded) =
      ( ( instancesIds
          |> Set.intersect selectedInstances
          |> (==) instancesIds
          |> (&&) (not (Set.isEmpty instancesIds))
        )
      , ( instancesIds
          |> Set.intersect expandedInstances
          |> (==) instancesIds
          |> (&&) (not (Set.isEmpty instancesIds))
        )
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
                , onCheck (AllInstancesSelected instancesIds)
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
                      instancesIds
                      (not allInstancesExpanded)
                    )
                ]
              ]
            , th
              -- [ width nameColumnWidth ]
              []
              [ icon "fa fa-hashtag" [ title "Instance ID" ] ]
            , th
              [ class "text-left hidden-xs"
              , width serviceColumnWidth
              ]
              [ icon "fa fa-cubes" [ title "Services" ] ]
            , th
              [ class "text-center hidden-sm hidden-xs"
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
          ( instances
            |> Dict.values
            |> List.concatMap (instanceRow selectedInstances expandedInstances instanceParameterForms visibleSecrets templates)
          )
        ]

instanceRow selectedInstances expandedInstances instanceParameterForms visibleSecrets templates instance =
  let
    ( instanceExpanded
    , instanceParameterForm
    ) =
    ( (Set.member instance.id expandedInstances)
    , (Dict.get instance.id instanceParameterForms)
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
      , td
        -- [ width nameColumnWidth ]
        []
        [ span
            [ attribute "role" "button"
            , onClick (InstanceExpanded instance.id (not instanceExpanded))
            ]
            [ text instance.id ]
        ]
      , td
        [ class "text-left hidden-xs"
        , width serviceColumnWidth
        ]
        ( servicesView instance.services )
      , td
        [ class "text-center hidden-sm hidden-xs"
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
        [ jobStatusView instance.jobStatus
        , text " "
        , iconButton
            "btn btn-default btn-xs"
            ( String.append
              "glyphicon glyphicon-"
              ( if ( instance.jobStatus == JobStatus.JobRunning
                    || instance.jobStatus == JobStatus.JobPending
                    || instance.jobStatus == JobStatus.JobDead) then
                  "refresh"
                else
                  "play"
              )
            )
            "Start Instance"
            ( List.append
              [ onClick (StartInstance instance.id) ]
              ( if (instance.jobStatus == JobStatus.JobUnknown) then
                  [ attribute "disabled" "disabled" ]
                else
                  []
              )
            )
        , text " "
        , iconButton
            "btn btn-default btn-xs"
            "glyphicon glyphicon-stop"
            "Stop Instance"
            ( List.append
              [ onClick (StopInstance instance.id) ]
              ( if (instance.jobStatus == JobStatus.JobStopped || instance.jobStatus == JobStatus.JobUnknown) then
                  [ attribute "disabled" "disabled" ]
                else
                  []
              )
            )
        ]
      ]
    ]
    ( if (instanceExpanded) then
        [ instanceDetailView
            instance
            instanceParameterForm
            visibleSecrets
            templates
        ]
      else
        []
    )

expandedTdStyle =
  [ ("border-top", "0px")
  , ("padding-top", "0px")
  ]

-- TODO as "id" is special we should treat it also special
instanceDetailView instance maybeInstanceParameterForm visibleSecrets templates =
  let periodicRuns =
    List.reverse (List.sortBy .utcSeconds instance.periodicRuns)
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
        ( List.append
          [ ParameterFormView.editView instance templates maybeInstanceParameterForm visibleSecrets
          ]
          ( if (List.isEmpty periodicRuns) then
              []
            else
              [ h5 [] [ text "Periodic Runs" ]
              , ul []
                (List.map periodicRunView periodicRuns)
              ]
          )
        )
      ]

periodicRunView periodicRun =
  li []
    [ code [ style [ ("margin-right", "12px" ) ] ] [ text periodicRun.jobName ]
    , text " "
    , span
      [ class "hidden-xs"
      , style [ ("margin-right", "12px" ) ]
      ]
      [ icon "fa fa-clock-o" []
      , text " "
      , text (periodicRunDateView (Date.fromTime (toFloat periodicRun.utcSeconds)))
      ]
    , text " "
    , jobStatusView periodicRun.status
    ]

periodicRunDateView date =
  String.concat
    [ toString (Date.hour date)
    , ":"
    , toString (Date.minute date)
    , ":"
    , toString (Date.second date)
    , ":"
    , toString (Date.millisecond date)
    , ", "
    , toString (Date.day date)
    , ". "
    , toString (Date.month date)
    , " "
    , toString (Date.year date)
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
      [ class ( String.concat [ "hidden-xs label label-", statusLabel ] )
      , style
        [ ("font-size", "90%")
        , ("width", "80px")
        , ("display", "inline-block")
        , ("margin-right", "8px")
        ]
      ]
      [ text statusText ]

servicesView services =
  if (List.isEmpty services) then
    [ text "-" ]
  else
    List.concatMap serviceView services

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
      ( List.append
        ( if (service.status /= ServiceUnknown) then
            [ href
              ( String.concat
                [ service.protocol
                , "://"
                , service.address
                , ":"
                , (toString service.port_)
                ]
              )
            ]
          else
            []
        )
        [ style
          [ ("margin-right", "8px")
          , ("color", textColor)
          ]
        , target "_blank"
        ]
      )
      [ icon iconClass [ style [ ("margin-right", "4px") ] ]
      , text service.name
      ]
    , text " "
    ]
