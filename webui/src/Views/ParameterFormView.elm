module Views.ParameterFormView exposing (editView, newView)

import Models.Resources.Instance exposing (..)
import Models.Resources.Template exposing (..)
import Models.Resources.Role as Role exposing (Role(..))
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Views.Styles exposing (instanceViewElementStyle)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Utils.ParameterUtils as ParameterUtils
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit, on)
import Dict exposing (..)
import Set exposing (Set)
import Maybe
import Maybe.Extra exposing (isJust, join)
import Json.Decode
import Regex exposing (contains, regex)
import Tuple exposing (first, second)


editingParamColor =
    "rgba(255, 177, 0, 0.46)"


normalParamColor =
    "#eee"


editView instance templates maybeInstanceParameterForm visibleSecrets maybeRole =
    let
        selectedTemplate =
            Maybe.andThen (\f -> f.selectedTemplate) maybeInstanceParameterForm

        formIsBeingEdited =
            isJust maybeInstanceParameterForm

        ( params, paramsHasError ) =
            parametersView
                "Parameters"
                instance
                instance.template
                maybeInstanceParameterForm
                visibleSecrets
                (not (isJust selectedTemplate) && (maybeRole == Just Administrator))

        ( newParams, newParamsHasError ) =
            selectedTemplate
                |> Maybe.map
                    (\t ->
                        parametersView
                            "New Parameters"
                            instance
                            t
                            maybeInstanceParameterForm
                            visibleSecrets
                            True
                    )
                |> Maybe.withDefault ( [], False )

        hasError =
            newParamsHasError || paramsHasError
    in
        Html.form
            [ onSubmit <|
                ApplyParameterValueChanges
                    instance
                    maybeInstanceParameterForm
                    (Maybe.withDefault instance.template selectedTemplate)
            ]
            (List.concat
                [ [ h5 [] [ text "Template" ]
                  , templateSelectionView instance.template selectedTemplate templates instance maybeRole
                  ]
                , params
                , newParams
                , if (maybeRole /= Just Administrator) then
                    []
                  else
                    [ div
                        [ class "row"
                        , style instanceViewElementStyle
                        ]
                        [ div
                            [ class "col-md-6" ]
                            [ iconButtonText
                                (if (formIsBeingEdited && (not hasError)) then
                                    "btn btn-success"
                                 else
                                    "btn btn-default"
                                )
                                "fa fa-check"
                                "Apply"
                                [ disabled ((not formIsBeingEdited) || hasError)
                                , type_ "submit"
                                ]
                            , text " "
                            , iconButtonText
                                (if (formIsBeingEdited) then
                                    "btn btn-warning"
                                 else
                                    "btn btn-default"
                                )
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
        otherParameters =
            template.parameters
                |> ParameterUtils.zipWithOrderIndex template.parameterInfos
                |> ParameterUtils.getOtherParametersSorted

        otherParameterValues =
            Dict.remove "id" instance.parameterValues

        otherParameterInfos =
            Dict.remove "id" template.parameterInfos
    in
        let
            ( otherParametersLeft, otherParametersRight ) =
                let
                    firstHalf =
                        otherParameters
                            |> List.length
                            |> toFloat
                            |> (\l -> l / 2)
                            |> ceiling
                in
                    ( List.take firstHalf otherParameters
                    , List.drop firstHalf otherParameters
                    )

            ( paramsLeft, leftHasError ) =
                editParameterValuesView
                    instance
                    otherParametersLeft
                    otherParameterValues
                    otherParameterInfos
                    maybeInstanceParameterForm
                    enabled
                    visibleSecrets

            ( paramsRight, rightHasError ) =
                editParameterValuesView
                    instance
                    otherParametersRight
                    otherParameterValues
                    otherParameterInfos
                    maybeInstanceParameterForm
                    enabled
                    visibleSecrets

            hasError =
                leftHasError || rightHasError
        in
            ( [ h5 [] [ text parametersH ]
              , div
                    [ class "row" ]
                    [ div
                        [ class "col-md-6" ]
                        (paramsLeft)
                    , div
                        [ class "col-md-6" ]
                        (paramsRight)
                    ]
              ]
            , hasError
            )


templateSelectionView currentTemplate selectedTemplate templates instance maybeRole =
    let
        templatesWithoutCurrentTemplate =
            Dict.filter (\k t -> t /= currentTemplate) templates
    in
        select
            (List.concat
                [ if (maybeRole /= Just Administrator) then
                    [ attribute "disabled" "disabled" ]
                  else
                    []
                , [ class "form-control"
                  , on "change" (Json.Decode.map (SelectEditInstanceTemplate instance templates) Html.Events.targetValue)
                  ]
                ]
            )
            (List.append
                [ templateOption currentTemplate selectedTemplate currentTemplate ]
                (templatesWithoutCurrentTemplate
                    |> Dict.values
                    |> List.map (templateOption currentTemplate selectedTemplate)
                )
            )


templateOption currentTemplate selectedTemplate template =
    let
        templateOption =
            if (currentTemplate == template) then
                "Unchanged"
            else if (currentTemplate.id == template.id) then
                "Upgrade to"
            else
                "Migrate to"
    in
        option
            [ value
                (if (currentTemplate == template) then
                    ""
                 else
                    template.id
                )
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
    let
        list =
            List.map
                (editParameterValueView
                    instance
                    parameterValues
                    parameterInfos
                    maybeInstanceParameterForm
                    enabled
                    visibleSecrets
                )
                parameters

        retList =
            List.map
                (\x -> first x)
                list

        hasError =
            List.any
                (\x -> second x)
                list
    in
        ( retList, hasError )


editParameterValueView :
    Instance
    -> Dict String (Maybe ParameterValue)
    -> Dict String ParameterInfo
    -> Maybe InstanceParameterForm
    -> Bool
    -> Set ( InstanceId, String )
    -> String
    -> ( Html UpdateBodyViewMsg, Bool )
editParameterValueView instance parameterValues parameterInfos maybeInstanceParameterForm enabled visibleSecrets parameter =
    let
        ( maybeParameterValue, maybeParameterInfo, maybeEditedValue ) =
            ( Dict.get parameter parameterValues
            , Dict.get parameter parameterInfos
            , join (Maybe.andThen (\f -> (Dict.get parameter f.changedParameterValues)) maybeInstanceParameterForm)
            )
    in
        let
            placeholderValue =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> (maybeValueToString i.default))
                    |> Maybe.withDefault ""

            parameterValue =
                case ( maybeEditedValue, maybeParameterValue ) of
                    ( Nothing, Nothing ) ->
                        ""

                    ( Just edited, _ ) ->
                        edited

                    ( Nothing, Just Nothing ) ->
                        "concealed"

                    -- See Instance.elm for why this is concealed
                    ( Nothing, Just (Just original) ) ->
                        valueToString original

            -- Since original is a ParameterValue need to convert it to string for display
            dataType =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> i.dataType)
                    |> Maybe.withDefault RawParam

            maybeErrMsg =
                maybeEditedValue
                    |> Maybe.andThen
                        (\value ->
                            case (valueFromString dataType value) of
                                Ok _ ->
                                    Nothing

                                -- we don't care about the value if it was ok. Just display the string.
                                Err msg ->
                                    Just msg
                        )

            hasError =
                isJust maybeErrMsg

            isSecret =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> i.secret)
                    |> Maybe.withDefault False

            secretVisible =
                Set.member ( instance.id, parameter ) visibleSecrets

            parameterName =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> i.name)
                    |> Maybe.withDefault parameter
        in
            ( p
                []
                (List.append
                    [ div
                        [ classList
                            [ ( "input-group", True )
                            , ( "has-error", hasError )
                            ]
                        ]
                        (List.append
                            [ span
                                [ class "input-group-addon"
                                , style
                                    [ ( "background-color", Maybe.withDefault normalParamColor (Maybe.map (\v -> editingParamColor) maybeEditedValue) )
                                    ]
                                ]
                                [ text parameterName ]
                            , input
                                [ type_
                                    (if (isSecret && (not secretVisible)) then
                                        "password"
                                     else
                                        "text"
                                    )
                                , class "form-control"
                                , attribute "aria-label" parameter
                                , placeholder placeholderValue
                                , value parameterValue
                                , disabled (not enabled)
                                , onInput (EnterEditInstanceParameterValue instance parameter)
                                ]
                                []
                            ]
                            (if (isSecret && enabled) then
                                [ a
                                    [ class "input-group-addon"
                                    , attribute "role" "button"
                                    , onClick (ToggleEditInstanceSecretVisibility instance.id parameter)
                                    ]
                                    [ icon
                                        (String.concat
                                            [ "glyphicon glyphicon-eye-"
                                            , (if secretVisible then
                                                "close"
                                               else
                                                "open"
                                              )
                                            ]
                                        )
                                        []
                                    ]
                                , a
                                    [ class "input-group-addon"
                                    , attribute "role" "button"
                                    , attribute
                                        "onclick"
                                        (String.concat
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
                    (case maybeErrMsg of
                        Nothing ->
                            []

                        Just msg ->
                            [ span
                                [ class "help-block" ]
                                [ text msg ]
                            ]
                    )
                )
            , hasError
            )


newView template maybeInstanceParameterForm visibleSecrets =
    let
        otherParameters =
            template.parameters
                |> ParameterUtils.zipWithOrderIndex template.parameterInfos
                |> ParameterUtils.getOtherParametersSorted

        otherParameterInfos =
            Dict.remove "id" template.parameterInfos

        instanceParameterForms =
            Maybe.withDefault InstanceParameterForm.empty maybeInstanceParameterForm
    in
        let
            ( otherParametersLeft, otherParametersRight, formBeingEdited ) =
                let
                    firstHalf =
                        otherParameters
                            |> List.length
                            |> toFloat
                            |> (\l -> l / 2)
                            |> ceiling
                in
                    ( List.take firstHalf otherParameters
                    , List.drop firstHalf otherParameters
                    , (not (Dict.isEmpty instanceParameterForms.changedParameterValues))
                    )

            ( paramsLeft, leftHaveError ) =
                newParameterValuesView
                    template
                    otherParametersLeft
                    otherParameterInfos
                    maybeInstanceParameterForm
                    visibleSecrets

            ( paramsRight, rightHaveError ) =
                newParameterValuesView
                    template
                    otherParametersRight
                    otherParameterInfos
                    maybeInstanceParameterForm
                    visibleSecrets

            ( idParam, idHasError ) =
                newParameterValueView
                    template
                    template.parameterInfos
                    maybeInstanceParameterForm
                    True
                    visibleSecrets
                    "id"

            hasError =
                leftHaveError || rightHaveError || idHasError
        in
            Html.form
                [ onSubmit (SubmitNewInstanceCreation template.id template.parameterInfos instanceParameterForms.changedParameterValues)
                , style
                    [ ( "padding-left", "40px" )
                    , ( "padding-right", "40px" )
                    ]
                , id <| String.concat [ "new-instance-form-", template.id ]
                ]
                [ h5 [] [ text "Parameters" ]
                , div
                    [ class "row" ]
                    [ div
                        [ class "col-md-6" ]
                        [ idParam ]
                    ]
                , div
                    [ class "row" ]
                    [ div
                        [ class "col-md-6" ]
                        (paramsLeft)
                    , div
                        [ class "col-md-6" ]
                        (paramsRight)
                    ]
                , div
                    [ class "row"
                    , style instanceViewElementStyle
                    ]
                    [ div
                        [ class "col-md-6" ]
                        [ iconButtonText
                            (if (not hasError) then
                                "btn btn-success"
                             else
                                "btn btn-default"
                            )
                            "fa fa-check"
                            "Apply"
                            [ disabled (hasError)
                            , type_ "submit"
                            ]
                        , text " "
                        , iconButtonText
                            "btn btn-warning"
                            "fa fa-ban"
                            "Discard"
                            [ onClick (DiscardNewInstanceCreation template.id)
                            , type_ "button"
                            , id <| String.concat [ "new-instance-form-discard-button-", template.id ]
                            ]
                        ]
                    ]
                ]


newParameterValuesView template parameters parameterInfos maybeInstanceParameterForm visibleSecrets =
    let
        list =
            List.map
                (newParameterValueView template parameterInfos maybeInstanceParameterForm True visibleSecrets)
                parameters

        retList =
            List.map
                (\x -> first x)
                list

        hasError =
            List.any
                (\x -> second x)
                list
    in
        ( retList, hasError )


newParameterValueView :
    Template
    -> Dict String ParameterInfo
    -> Maybe InstanceParameterForm
    -> Bool
    -> Set ( InstanceId, String )
    -> String
    -> ( Html UpdateBodyViewMsg, Bool )
newParameterValueView template parameterInfos maybeInstanceParameterForm enabled visibleSecrets parameter =
    let
        ( maybeParameterInfo, maybeEditedValue ) =
            ( Dict.get parameter parameterInfos
            , join (Maybe.andThen (\f -> (Dict.get parameter f.changedParameterValues)) maybeInstanceParameterForm)
            )
    in
        let
            placeholderValue =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> (maybeValueToString i.default))
                    |> Maybe.withDefault ""

            parameterValue =
                maybeEditedValue
                    |> Maybe.withDefault ""

            dataType =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> i.dataType)
                    |> Maybe.withDefault RawParam

            isSecret =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> i.secret)
                    |> Maybe.withDefault False

            secretVisible =
                Set.member ( template.id, parameter ) visibleSecrets

            parameterName =
                maybeParameterInfo
                    |> Maybe.andThen (\i -> i.name)
                    |> Maybe.withDefault parameter

            maybeErrMsg =
                maybeEditedValue
                    |> Maybe.andThen
                        (\value ->
                            case (valueFromString dataType value) of
                                Ok _ ->
                                    Nothing

                                -- we don't care about the value if it was ok. Just display the string.
                                Err msg ->
                                    Just msg
                        )

            hasError =
                isJust maybeErrMsg
        in
            ( p
                []
                (List.append
                    [ div
                        [ classList
                            [ ( "input-group", True )
                            , ( "has-error", hasError )
                            ]
                        , id <| String.concat [ "new-instance-form-input-group-", template.id, "-", parameter ]
                        ]
                        (List.append
                            [ span
                                [ class "input-group-addon"
                                , style
                                    [ ( "background-color", Maybe.withDefault normalParamColor (Maybe.map (\v -> editingParamColor) maybeEditedValue) )
                                    ]
                                ]
                                [ text parameterName ]
                            , input
                                [ type_
                                    (if (isSecret && (not secretVisible)) then
                                        "password"
                                     else
                                        "text"
                                    )
                                , class "form-control"
                                , attribute "aria-label" parameter
                                , placeholder placeholderValue
                                , value parameterValue
                                , disabled (not enabled)
                                , id <| String.concat [ "new-instance-form-parameter-input-", template.id, "-", parameter ]
                                , onInput (EnterNewInstanceParameterValue template.id parameter)
                                ]
                                []
                            ]
                            (if (isSecret) then
                                [ a
                                    [ class "input-group-addon"
                                    , attribute "role" "button"
                                    , onClick (ToggleNewInstanceSecretVisibility template.id parameter)
                                    , id <| String.concat [ "new-instance-form-parameter-secret-visibility-", template.id, "-", parameter ]
                                    ]
                                    [ icon
                                        (String.concat
                                            [ "glyphicon glyphicon-eye-"
                                            , (if secretVisible then
                                                "close"
                                               else
                                                "open"
                                              )
                                            ]
                                        )
                                        []
                                    ]
                                , a
                                    [ class "input-group-addon"
                                    , attribute "role" "button"
                                    , attribute
                                        "onclick"
                                        (String.concat
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
                    (maybeErrMsg
                        |> Maybe.andThen
                            (\errMsg ->
                                Just
                                    [ span
                                        [ class "help-block" ]
                                        [ text errMsg ]
                                    ]
                            )
                        |> Maybe.withDefault []
                    )
                )
            , hasError
            )
