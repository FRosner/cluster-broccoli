module Views.ParameterFormView exposing (editView, newView)

import Models.Resources.Instance exposing (..)
import Models.Resources.Template exposing (..)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)

import Updates.Messages exposing (UpdateBodyViewMsg(..))

import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Utils.MaybeUtils as MaybeUtils

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit, on)

import Dict exposing (..)

import Set exposing (Set)

import Maybe

import Json.Decode

editingParamColor = "rgba(255, 177, 0, 0.46)"
normalParamColor = "#eee"

editView instance templates maybeInstanceParameterForm visibleSecrets =
  let
    selectedTemplate = Maybe.andThen (\f -> f.selectedTemplate) maybeInstanceParameterForm
    formIsBeingEdited = MaybeUtils.isDefined maybeInstanceParameterForm
  in
    Html.form
      [ onSubmit <|
          ApplyParameterValueChanges
            instance
            maybeInstanceParameterForm
            ( Maybe.withDefault instance.template selectedTemplate )
      ]
      ( List.concat
        [ [ h5 [] [ text "Template" ]
          , templateSelectionView instance.template selectedTemplate templates instance
          ]
        , parametersView "Parameters" instance instance.template maybeInstanceParameterForm visibleSecrets (not (MaybeUtils.isDefined selectedTemplate))
        , selectedTemplate
          |> Maybe.map (\t -> parametersView "New Parameters" instance t maybeInstanceParameterForm visibleSecrets True)
          |> Maybe.withDefault []
        , [ div
            [ class "row"
            , style [ ("margin-bottom", "15px") ]
            ]
            [ div
              [ class "col-md-6" ]
              [ iconButtonText
                  ( if (formIsBeingEdited) then "btn btn-success" else "btn btn-default" )
                  "fa fa-check"
                  "Apply"
                  [ disabled (not formIsBeingEdited)
                  , type_ "submit"
                  ]
              , text " "
              , iconButtonText
                  ( if (formIsBeingEdited) then "btn btn-warning" else "btn btn-default" )
                  "fa fa-ban"
                  "Discard"
                  [ disabled (not formIsBeingEdited)
                  , type_ "button"
                  , onClick (DiscardParameterValueChanges instance.id)
                  ]
              ]
            ]
          ]
        ]
      )

parametersView parametersH instance template maybeInstanceParameterForm visibleSecrets enabled =
  let
    otherParameters = List.filter (\p -> p /= "id") template.parameters
    otherParameterValues = Dict.remove "id" instance.parameterValues
    otherParameterInfos = Dict.remove "id" template.parameterInfos
  in
    let
      ( otherParametersLeft
      , otherParametersRight
      ) =
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
      [ h5 [] [ text parametersH ]
      , div
        [ class "row" ]
        [ div
          [ class "col-md-6" ]
          ( editParameterValuesView instance otherParametersLeft otherParameterValues otherParameterInfos maybeInstanceParameterForm enabled visibleSecrets )
        , div
          [ class "col-md-6" ]
          ( editParameterValuesView instance otherParametersRight otherParameterValues otherParameterInfos maybeInstanceParameterForm enabled visibleSecrets )
        ]
      ]

templateSelectionView currentTemplate selectedTemplate templates instance =
  let templatesWithoutCurrentTemplate =
    Dict.filter (\k t -> t /= currentTemplate) templates
  in
    select
      [ class "form-control"
      , on "change" (Json.Decode.map (SelectEditInstanceTemplate instance templates) Html.Events.targetValue)
      ]
      ( List.append
        [ templateOption currentTemplate selectedTemplate currentTemplate ]
        ( templatesWithoutCurrentTemplate
          |> Dict.values
          |> List.map (templateOption currentTemplate selectedTemplate)
        )
      )

templateOption currentTemplate selectedTemplate template =
  let templateOption =
    if (currentTemplate == template) then
      "Unchanged"
    else if (currentTemplate.id == template.id) then
      "Upgrade to"
    else
      "Migrate to"
  in
    option
      [ value (if (currentTemplate == template) then "" else template.id)
      , selected (selectedTemplate == Just template)
      ]
      [ text templateOption
      , text ": "
      , text template.id
      , text " ("
      , text template.version
      , text ")"
      ]

editParameterValuesView instance parameters parameterValues parameterInfos maybeInstanceParameterForm enabled visibleSecrets =
  List.map
    ( editParameterValueView instance parameterValues parameterInfos maybeInstanceParameterForm enabled visibleSecrets )
    parameters

editParameterValueView : Instance -> Dict String String -> Dict String ParameterInfo -> Maybe InstanceParameterForm -> Bool -> Set (InstanceId, String) -> String -> Html UpdateBodyViewMsg
editParameterValueView instance parameterValues parameterInfos maybeInstanceParameterForm enabled visibleSecrets parameter =
  let
    ( maybeParameterValue
    , maybeParameterInfo
    , maybeEditedValue
    ) =
    ( Dict.get parameter parameterValues
    , Dict.get parameter parameterInfos
    , Maybe.andThen (\f -> (Dict.get parameter f.changedParameterValues)) maybeInstanceParameterForm
    )
  in
    let
      ( placeholderValue
      , parameterValue
      , isSecret
      , secretVisible
      ) =
      ( maybeParameterInfo
          |> Maybe.andThen (\i -> i.default)
          |> Maybe.withDefault ""
      , maybeEditedValue
          |> Maybe.withDefault (Maybe.withDefault "" maybeParameterValue)
      , maybeParameterInfo
          |> Maybe.andThen (\i -> i.secret)
          |> Maybe.withDefault False
      , Set.member (instance.id, parameter) visibleSecrets
      )
    in
      p
        []
        [ div
          [ class "input-group" ]
          ( List.append
            [ span
              [ class "input-group-addon"
              , style
                [ ( "background-color", Maybe.withDefault normalParamColor (Maybe.map (\v -> editingParamColor) maybeEditedValue) )
                ]
              ]
              [ text parameter ]
            , input
              [ type_ ( if ( isSecret && ( not secretVisible ) ) then "password" else "text" )
              , class "form-control"
              , attribute "aria-label" parameter
              , placeholder placeholderValue
              , value parameterValue
              , disabled (not enabled)
              , onInput (EnterEditInstanceParameterValue instance parameter)
              ]
              []
            ]
            ( if (isSecret) then
                [ a
                  [ class "input-group-addon"
                  , attribute "role" "button"
                  , onClick ( ToggleEditInstanceSecretVisibility instance.id parameter )
                  ]
                  [ icon
                    ( String.concat
                      [ "glyphicon glyphicon-eye-"
                      , ( if secretVisible then "close" else "open" )
                      ]
                    )
                    []
                  ]
                , a
                  [ class "input-group-addon"
                  , attribute "role" "button"
                , attribute
                    "onclick"
                    ( String.concat
                      [ "copy('"
                      , parameterValue
                      , "')"
                      ]
                    )
                  ]
                  [ icon "glyphicon glyphicon-copy" [] ]
                ]
              else
                []
            )
          )
        ]

-- TODO editParameterValueView needs to be adapted (probably copy paste for now?) so it can work without an instance as we don't have one here
newView template maybeInstanceParameterForm visibleSecrets =
  let
    ( otherParameters
    , otherParameterInfos
    , instanceParameterForms
    ) =
    ( List.filter (\p -> p /= "id") template.parameters
    , Dict.remove "id" template.parameterInfos
    , Maybe.withDefault InstanceParameterForm.empty maybeInstanceParameterForm
    )
  in
    let
      ( otherParametersLeft
      , otherParametersRight
      , formBeingEdited
      ) =
      let firstHalf =
        otherParameters
          |> List.length
          |> toFloat
          |> (\l -> l / 2)
          |> ceiling
      in
        ( List.take firstHalf otherParameters
        , List.drop firstHalf otherParameters
        , ( not (Dict.isEmpty instanceParameterForms.changedParameterValues) )
        )
    in
      Html.form
        [ onSubmit (SubmitNewInstanceCreation template.id instanceParameterForms.changedParameterValues)
        , style
          [ ("padding-left", "40px")
          , ("padding-right", "40px")
          ]
        ]
        [ h5 [] [ text "Parameters" ]
        , div
          [ class "row" ]
          [ div
            [ class "col-md-6" ]
            [ ( newParameterValueView template template.parameterInfos maybeInstanceParameterForm True visibleSecrets "id" ) ]
          ]
        , div
          [ class "row" ]
          [ div
            [ class "col-md-6" ]
            ( newParameterValuesView template otherParametersLeft otherParameterInfos maybeInstanceParameterForm visibleSecrets )
          , div
            [ class "col-md-6" ]
            ( newParameterValuesView template otherParametersRight otherParameterInfos maybeInstanceParameterForm visibleSecrets )
          ]
        , div
          [ class "row"
          , style [ ("margin-bottom", "15px") ]
          ]
          [ div
            [ class "col-md-6" ]
            [ iconButtonText
                "btn btn-success"
                "fa fa-check"
                "Apply"
                [ type_ "submit" ]
            , text " "
            , iconButtonText
                "btn btn-warning"
                "fa fa-ban"
                "Discard"
                [ onClick (DiscardNewInstanceCreation template.id)
                , type_ "button"
                ]
            ]
          ]
        ]

newParameterValuesView template parameters parameterInfos maybeInstanceParameterForm visibleSecrets =
  List.map
    ( newParameterValueView template parameterInfos maybeInstanceParameterForm True visibleSecrets )
    parameters


newParameterValueView : Template -> Dict String ParameterInfo -> Maybe InstanceParameterForm -> Bool -> Set (InstanceId, String) -> String -> Html UpdateBodyViewMsg
newParameterValueView template parameterInfos maybeInstanceParameterForm enabled visibleSecrets parameter =
  let
    ( maybeParameterInfo
    , maybeEditedValue
    ) =
    ( Dict.get parameter parameterInfos
    , Maybe.andThen (\f -> (Dict.get parameter f.changedParameterValues)) maybeInstanceParameterForm
    )
  in
    let
      ( placeholderValue
      , parameterValue
      , isSecret
      , secretVisible
      ) =
      ( maybeParameterInfo
          |> Maybe.andThen (\i -> i.default)
          |> Maybe.withDefault ""
      , maybeEditedValue
          |> Maybe.withDefault ""
      , maybeParameterInfo
          |> Maybe.andThen (\i -> i.secret)
          |> Maybe.withDefault False
      , Set.member (template.id, parameter) visibleSecrets
      )
    in
      p
        []
        [ div
          [ class "input-group" ]
          ( List.append
            [ span
              [ class "input-group-addon"
              , style
                [ ( "background-color", Maybe.withDefault normalParamColor (Maybe.map (\v -> editingParamColor) maybeEditedValue) )
                ]
              ]
              [ text parameter ]
            , input
              [ type_ ( if ( isSecret && ( not secretVisible ) ) then "password" else "text" )
              , class "form-control"
              , attribute "aria-label" parameter
              , placeholder placeholderValue
              , value parameterValue
              , disabled (not enabled)
              , onInput (EnterNewInstanceParameterValue template parameter)
              ]
              []
            ]
            ( if (isSecret) then
                [ a
                  [ class "input-group-addon"
                  , attribute "role" "button"
                  , onClick ( ToggleNewInstanceSecretVisibility template.id parameter )
                  ]
                  [ icon
                    ( String.concat
                      [ "glyphicon glyphicon-eye-"
                      , ( if secretVisible then "close" else "open" )
                      ]
                    )
                    []
                  ]
                , a
                  [ class "input-group-addon"
                  , attribute "role" "button"
                , attribute
                    "onclick"
                    ( String.concat
                      [ "copy('"
                      , parameterValue
                      , "')"
                      ]
                    )
                  ]
                  [ icon "glyphicon glyphicon-copy" [] ]
                ]
              else
                []
            )
          )
        ]