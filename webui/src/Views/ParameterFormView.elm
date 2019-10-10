module Views.ParameterFormView exposing (editView, newView)

import Bootstrap.Form as Form
import Bootstrap.Form.Select as Select
import Dict exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (on, onCheck, onClick, onInput, onSubmit)
import Json.Decode
import Maybe
import Maybe.Extra exposing (isJust, join)
import Models.Resources.Instance exposing (..)
import Models.Resources.Role as Role exposing (Role(..))
import Models.Resources.Template exposing (..)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Set exposing (Set)
import Tuple exposing (first, second)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButton, iconButtonText)
import Utils.ParameterUtils as ParameterUtils
import Views.Styles exposing (instanceViewElementStyle)


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
                "Parameterss"
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
            [ [ h6 [] [ text "Template" ]
              , templateSelectionView instance.template selectedTemplate templates instance maybeRole
              ]
            , params
            , newParams
            , if maybeRole /= Just Administrator then
                []

              else
                [ div
                    [ class "row"
                    , style instanceViewElementStyle
                    ]
                    [ div
                        [ class "col-md-6" ]
                        [ iconButtonText
                            (if formIsBeingEdited && not hasError then
                                "btn btn-success"

                             else
                                "btn btn-outline-secondary"
                            )
                            "fa fa-check"
                            "Apply"
                            [ disabled (not formIsBeingEdited || hasError)
                            , type_ "submit"
                            ]
                        , text " "
                        , iconButtonText
                            (if formIsBeingEdited then
                                "btn btn-warning"

                             else
                                "btn btn-outline-secondary"
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
    ( [ h6 [] [ text parametersH ]
      , div
            [ class "row" ]
            [ div
                [ class "col-md-6" ]
                paramsLeft
            , div
                [ class "col-md-6" ]
                paramsRight
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
            [ if maybeRole /= Just Administrator then
                [ attribute "disabled" "disabled" ]

              else
                []
            , [ class "form-control"
              , on "change" (Json.Decode.map (SelectEditInstanceTemplate instance templates) Html.Events.targetValue)
              ]
            ]
        )
        (List.concat
            [ [ templateOption currentTemplate selectedTemplate currentTemplate ]
            , templatesWithoutCurrentTemplate
                |> Dict.values
                |> List.map (templateOption currentTemplate selectedTemplate)
            ]
        )


templateOption currentTemplate selectedTemplate template =
    let
        templateOption =
            if currentTemplate == template then
                "Unchanged"

            else if currentTemplate.id == template.id then
                "Upgrade to"

            else
                "Migrate to"
    in
    option
        [ value
            (if currentTemplate == template then
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
            , join (Maybe.andThen (\f -> Dict.get parameter f.changedParameterValues) maybeInstanceParameterForm)
            )
    in
    let
        placeholderValue =
            maybeParameterInfo
                |> Maybe.andThen (\i -> maybeValueToString i.default)
                |> Maybe.withDefault ""

        parameterValue =
            case ( maybeEditedValue, maybeParameterValue ) of
                ( Nothing, Nothing ) ->
                    ""

                ( Just edited, _ ) ->
                    edited

                -- See Instance.elm for why this is concealed
                ( Nothing, Just Nothing ) ->
                    "concealed"

                ( Nothing, Just (Just original) ) ->
                    valueToString original

        -- Since original is a ParameterValue need to convert it to string for display
        dataType =
            maybeParameterInfo
                |> Maybe.map (\i -> i.dataType)
                -- The default statement will never be called. We use this only because Map.get returns Maybe
                -- and we want to flatten it
                |> Maybe.withDefault StringParam

        maybeErrMsg =
            getErrorMessage maybeEditedValue parameterName dataType

        hasError =
            isJust maybeErrMsg

        inputErrorClass =
            if hasError then
                "is-invalid"

            else
                ""

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
        [ div
            [ class "input-group" ]
            (List.concat
                [ [ div
                        [ class "input-group-prepend" ]
                        [ div
                            [ class "input-group-text"
                            , style
                                [ ( "background-color"
                                  , Maybe.withDefault
                                        normalParamColor
                                        (Maybe.map (\v -> editingParamColor) maybeEditedValue)
                                  )
                                ]
                            ]
                            [ text parameterName
                            , text " "
                            , sup [] [ text (dataTypeToTitle dataType) ]
                            ]
                        ]
                  , inputParameterValueView isSecret
                        secretVisible
                        inputErrorClass
                        parameter
                        placeholderValue
                        parameterValue
                        enabled
                        maybeParameterInfo
                        (String.concat [ "edit-instance-form-parameter-input-", instance.id, "-", instance.template.id, "-", parameter ])
                        (EnterEditInstanceParameterValue instance parameter)
                  ]
                , if isSecret && enabled then
                    [ div
                        [ class "input-group-append" ]
                        [ a
                            [ attribute "role" "button"
                            , class "btn btn-outline-secondary"
                            , onClick (ToggleEditInstanceSecretVisibility instance.id parameter)
                            ]
                            [ icon
                                (String.concat
                                    [ "fa fa-eye"
                                    , if secretVisible then
                                        "-slash"

                                      else
                                        ""
                                    ]
                                )
                                []
                            ]
                        , a
                            [ attribute "role" "button"
                            , class "btn btn-outline-secondary"
                            , attribute
                                "onClick"
                                (String.concat
                                    [ "copy('"
                                    , parameterValue
                                    , "')"
                                    ]
                                )
                            ]
                            [ icon "fa fa-clipboard" [] ]
                        ]
                    ]

                  else
                    []
                , case maybeErrMsg of
                    Nothing ->
                        []

                    Just msg ->
                        [ span
                            [ class "invalid-feedback"
                            , id <| String.concat [ "edit-instance-form-parameter-input-error-", instance.id, "-", instance.template.id, "-", parameter ]
                            ]
                            [ text msg ]
                        ]
                ]
            )
        ]
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
            , not (Dict.isEmpty instanceParameterForms.changedParameterValues)
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
        [ h6 [] [ text "Parameters" ]
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
                paramsLeft
            , div
                [ class "col-md-6" ]
                paramsRight
            ]
        , div
            [ class "row"
            , style instanceViewElementStyle
            ]
            [ div
                [ class "col-md-6" ]
                [ iconButtonText
                    (if not hasError then
                        "btn btn-success"

                     else
                        "btn btn-outline-secondary"
                    )
                    "fa fa-check"
                    "Apply"
                    [ disabled hasError
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
            , join (Maybe.andThen (\f -> Dict.get parameter f.changedParameterValues) maybeInstanceParameterForm)
            )
    in
    let
        placeholderValue =
            maybeParameterInfo
                |> Maybe.andThen (\i -> maybeValueToString i.default)
                |> Maybe.withDefault ""

        parameterValue =
            maybeEditedValue
                |> Maybe.withDefault ""

        dataType =
            maybeParameterInfo
                |> Maybe.map (\i -> i.dataType)
                -- The default statement will never be called. We use this only because Map.get returns Maybe
                |> Maybe.withDefault StringParam

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
            getErrorMessage maybeEditedValue parameterName dataType

        hasError =
            isJust maybeErrMsg

        inputErrorClass =
            if hasError then
                "is-invalid"

            else
                ""

        inputId =
            String.concat [ "new-instance-form-parameter-input-", template.id, "-", parameter ]
    in
    ( p
        []
        [ div
            [ class "input-group"
            , id <| String.concat [ "new-instance-form-input-group-", template.id, "-", parameter ]
            ]
            (List.concat
                [ [ div
                        [ class "input-group-prepend" ]
                        [ div
                            [ class "input-group-text"
                            , style
                                [ ( "background-color"
                                  , Maybe.withDefault
                                        normalParamColor
                                        (Maybe.map (\v -> editingParamColor) maybeEditedValue)
                                  )
                                ]
                            ]
                            [ text parameterName
                            , text " "
                            , sup [] [ text (dataTypeToTitle dataType) ]
                            ]
                        ]
                  , inputParameterValueView
                        isSecret
                        secretVisible
                        inputErrorClass
                        parameter
                        placeholderValue
                        parameterValue
                        enabled
                        maybeParameterInfo
                        (String.concat [ "new-instance-form-parameter-input-", template.id, "-", parameter ])
                        (EnterNewInstanceParameterValue template.id parameter)
                  ]
                , if isSecret then
                    [ div
                        [ class "input-group-append" ]
                        [ a
                            [ attribute "role" "button"
                            , class "btn btn-outline-secondary"
                            , onClick (ToggleNewInstanceSecretVisibility template.id parameter)
                            , id <|
                                String.concat
                                    [ "new-instance-form-parameter-secret-visibility-"
                                    , template.id
                                    , "-"
                                    , parameter
                                    ]
                            ]
                            [ icon
                                (String.concat
                                    [ "fa fa-eye"
                                    , if secretVisible then
                                        "-slash"

                                      else
                                        ""
                                    ]
                                )
                                []
                            ]
                        , a
                            [ attribute "role" "button"
                            , class "btn btn-outline-secondary"
                            , attribute
                                "onClick"
                                (String.concat
                                    [ "copy('"
                                    , parameterValue
                                    , "')"
                                    ]
                                )
                            ]
                            [ icon "fa fa-clipboard" [] ]
                        ]
                    ]

                  else
                    []
                , case maybeErrMsg of
                    Nothing ->
                        []

                    Just msg ->
                        [ span
                            [ class "invalid-feedback"
                            , id <| String.concat [ "new-instance-form-parameter-input-error-", template.id, "-", parameter ]
                            ]
                            [ text msg ]
                        ]
                ]
            )
        ]
    , hasError
    )


inputParameterValueView :
    Bool
    -> Bool
    -> String
    -> String
    -> String
    -> String
    -> Bool
    -> Maybe ParameterInfo
    -> String
    -> (String -> UpdateBodyViewMsg)
    -> Html UpdateBodyViewMsg
inputParameterValueView isSecret secretVisible inputErrorClass parameter placeholderValue parameterValue enabled maybeParameterInfo inputId updateMessageFunc =
    if
        isSimpleDataType
            (maybeParameterInfo |> Maybe.andThen (\i -> Just i.dataType) |> Maybe.withDefault StringParam)
    then
        input
            [ type_
                (if isSecret && not secretVisible then
                    "password"

                 else
                    "text"
                )
            , class (String.concat [ "form-control ", inputErrorClass ])
            , attribute "aria-label" parameter
            , placeholder placeholderValue
            , value parameterValue
            , disabled (not enabled)
            , id inputId
            , onInput updateMessageFunc
            ]
            []

    else
        Select.select
            [ Select.id inputId, Select.onChange updateMessageFunc ]
            (maybeParameterInfo
                |> Maybe.andThen
                    (\i ->
                        case i.dataType of
                            DecimalSetParam decimalSet ->
                                Just (selectOptionView decimalSet parameterValue toString)

                            IntSetParam intSet ->
                                Just (selectOptionView intSet parameterValue toString)

                            StringSetParam stringSet ->
                                Just (selectOptionView stringSet parameterValue identity)

                            _ ->
                                Nothing
                    )
                |> Maybe.withDefault []
            )


selectOptionView options parameterValue toStringFunc =
    List.map
        (\itm ->
            Select.item
                [ selected (toStringFunc itm == parameterValue) ]
                [ text (toStringFunc itm) ]
        )
        options


isSimpleDataType : ParameterType -> Bool
isSimpleDataType dataType =
    case dataType of
        RawParam ->
            True

        StringParam ->
            True

        IntParam ->
            True

        DecimalParam ->
            True

        DecimalSetParam _ ->
            False

        IntSetParam _ ->
            False

        StringSetParam _ ->
            False


getErrorMessage : Maybe String -> String -> ParameterType -> Maybe String
getErrorMessage maybeValue paramName dataType =
    maybeValue
        |> Maybe.andThen
            (\value ->
                case value of
                    "" ->
                        Nothing

                    _ ->
                        case valueFromString dataType value of
                            -- The error is not present if the value was okay
                            Ok _ ->
                                Nothing

                            Err _ ->
                                Just (String.concat <| [ paramName, " must be ", dataTypeToString dataType ])
            )


dataTypeToTitle : ParameterType -> String
dataTypeToTitle dataType =
    String.concat
        [ "("
        , case dataType of
            StringParam ->
                "String"

            RawParam ->
                "Raw"

            IntParam ->
                "Integer"

            DecimalParam ->
                "Decimal"

            StringSetParam _ ->
                "String"

            DecimalSetParam _ ->
                "Decimal"

            IntSetParam _ ->
                "Int"
        , ")"
        ]
