module Updates.UpdateBodyView exposing (updateBodyView)

import Dict exposing (Dict)
import Messages exposing (..)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Models.Resources.InstanceUpdate as InstanceUpdate exposing (InstanceUpdate)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Resources.Template as Template exposing (..)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Set exposing (Set)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.CmdUtils as CmdUtils
import Utils.DictUtils exposing (flatMap)


updateBodyView : UpdateBodyViewMsg -> BodyUiModel -> ( BodyUiModel, Cmd AnyMsg )
updateBodyView message oldBodyUiModel =
    let
        ( oldExpandedTemplates, oldSelectedInstances, oldExpandedInstances, oldInstanceParameterForms, oldEditInstanceVisibleSecrets, oldNewTemplateVisibleSecrets, oldExpandedNewInstanceForms ) =
            ( oldBodyUiModel.expandedTemplates
            , oldBodyUiModel.selectedInstances
            , oldBodyUiModel.expandedInstances
            , oldBodyUiModel.instanceParameterForms
            , oldBodyUiModel.visibleEditInstanceSecrets
            , oldBodyUiModel.visibleNewInstanceSecrets
            , oldBodyUiModel.expandedNewInstanceForms
            )
    in
    case message of
        ToggleTemplate templateId ->
            let
                newExpandedTemplates =
                    insertOrRemove (not (Set.member templateId oldExpandedTemplates)) templateId oldExpandedTemplates
            in
            ( { oldBodyUiModel
                | expandedTemplates = newExpandedTemplates
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        ToggleNodeAllocation nodeId ->
            let
                oldTemporaryStates =
                    oldBodyUiModel.temporaryStates

                oldExpandedResourceAllocs =
                    oldTemporaryStates.expandedResourceAllocs

                newExpandedResourceAllocs =
                    insertOrRemove (not (Set.member nodeId oldExpandedResourceAllocs)) nodeId oldExpandedResourceAllocs
            in
            ( { oldBodyUiModel
                | temporaryStates = { oldTemporaryStates | expandedResourceAllocs = newExpandedResourceAllocs }
              }
            , Cmd.none
            )

        InstanceSelected instanceId selected ->
            let
                newSelectedInstances =
                    insertOrRemove selected instanceId oldSelectedInstances
            in
            ( { oldBodyUiModel
                | selectedInstances = newSelectedInstances
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        AllInstancesSelected instanceIds selected ->
            let
                newSelectedInstances =
                    unionOrDiff selected oldSelectedInstances instanceIds
            in
            ( { oldBodyUiModel
                | selectedInstances = newSelectedInstances
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        InstanceExpanded instanceId expanded ->
            let
                newExpandedInstances =
                    insertOrRemove expanded instanceId oldExpandedInstances

                command =
                    if expanded then
                        CmdUtils.sendMsg (SendWsMsg (GetInstanceTasks instanceId))

                    else
                        Cmd.none
            in
            ( { oldBodyUiModel
                | expandedInstances = newExpandedInstances
                , attemptedDeleteInstances = Nothing
              }
            , command
            )

        AllInstancesExpanded instanceIds expanded ->
            let
                newExpandedInstances =
                    unionOrDiff expanded oldExpandedInstances instanceIds

                command =
                    if expanded then
                        Set.toList instanceIds
                            |> List.map (GetInstanceTasks >> SendWsMsg >> CmdUtils.sendMsg)
                            |> Cmd.batch

                    else
                        Cmd.none
            in
            ( { oldBodyUiModel
                | expandedInstances = newExpandedInstances
                , attemptedDeleteInstances = Nothing
              }
            , command
            )

        EnterEditInstanceParameterValue instance parameter value ->
            let
                newInstanceParameterForms =
                    Dict.update
                        instance.id
                        (addParameterValue instance parameter value)
                        oldInstanceParameterForms
            in
            ( { oldBodyUiModel
                | instanceParameterForms = newInstanceParameterForms
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        SelectEditInstanceTemplate instance templates templateId ->
            let
                newInstanceParameterForms =
                    Dict.update
                        instance.id
                        (selectTemplate instance templates templateId)
                        oldInstanceParameterForms
            in
            ( { oldBodyUiModel
                | instanceParameterForms = newInstanceParameterForms
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        EnterNewInstanceParameterValue templateId parameter value ->
            let
                newExpandedNewInstanceForms =
                    Dict.update
                        templateId
                        (updateNewInstanceParameterForm parameter value)
                        oldExpandedNewInstanceForms
            in
            ( { oldBodyUiModel
                | expandedNewInstanceForms = newExpandedNewInstanceForms
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        DiscardParameterValueChanges instanceId ->
            ( { oldBodyUiModel
                | instanceParameterForms = resetEditParameterForm instanceId oldInstanceParameterForms
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        ApplyParameterValueChanges instance maybeInstanceParameterForm template ->
            let
                message =
                    InstanceUpdate
                        instance.id
                        Nothing
                        (Maybe.map
                            (\f ->
                                f.originalParameterValues
                                    |> Dict.union f.changedParameterValues
                                    |> Dict.filter (\p v -> List.member p template.parameters)
                                    |> flatMap (mapStringToParamVal template.parameterInfos)
                            )
                            maybeInstanceParameterForm
                        )
                        (Maybe.andThen (\f -> Maybe.map (\t -> t.id) f.selectedTemplate) maybeInstanceParameterForm)
                        Nothing
            in
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , CmdUtils.sendMsg (SendWsMsg (UpdateInstanceMessage message))
            )

        ToggleEditInstanceSecretVisibility instanceId parameter ->
            let
                newVisibleSecrets =
                    insertOrRemove
                        (not (Set.member ( instanceId, parameter ) oldEditInstanceVisibleSecrets))
                        ( instanceId, parameter )
                        oldEditInstanceVisibleSecrets
            in
            ( { oldBodyUiModel
                | visibleEditInstanceSecrets = newVisibleSecrets
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        ToggleNewInstanceSecretVisibility templateId parameter ->
            let
                newVisibleSecrets =
                    insertOrRemove
                        (not (Set.member ( templateId, parameter ) oldNewTemplateVisibleSecrets))
                        ( templateId, parameter )
                        oldNewTemplateVisibleSecrets
            in
            ( { oldBodyUiModel
                | visibleNewInstanceSecrets = newVisibleSecrets
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        ExpandNewInstanceForm expanded templateId ->
            let
                newExpandedNewInstanceForms =
                    if expanded then
                        Dict.update
                            templateId
                            (expandParameterForm templateId oldExpandedNewInstanceForms)
                            oldExpandedNewInstanceForms

                    else
                        Dict.remove templateId oldExpandedNewInstanceForms
            in
            ( { oldBodyUiModel
                | expandedNewInstanceForms = newExpandedNewInstanceForms
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        SubmitNewInstanceCreation templateId parameterInfos parameterValues ->
            let
                setDefaults =
                    Dict.filter
                        (\paramName paramInfo ->
                            case paramInfo.dataType of
                                DecimalSetParam _ ->
                                    True

                                IntSetParam _ ->
                                    True

                                StringSetParam _ ->
                                    True

                                _ ->
                                    False
                        )
                        parameterInfos
                        |> Dict.map
                            (\paramName paramInfo ->
                                case paramInfo.dataType of
                                    DecimalSetParam values ->
                                        List.head values |> Maybe.andThen (\val -> Just (DecimalVal val))

                                    IntSetParam values ->
                                        List.head values |> Maybe.andThen (\val -> Just (IntVal val))

                                    StringSetParam values ->
                                        List.head values |> Maybe.andThen (\val -> Just (StringVal val))

                                    _ ->
                                        Nothing
                            )
                        |> flatMap (\c b -> b)

                parameterValuesTransformed =
                    parameterValues
                        |> flatMap (mapStringToParamVal parameterInfos)

                message =
                    setDefaults
                        |> Dict.union parameterValuesTransformed
                        |> InstanceCreation templateId
            in
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , CmdUtils.sendMsg (SendWsMsg (AddInstanceMessage message))
            )

        StartInstance instanceId ->
            let
                message =
                    InstanceUpdate instanceId (Just JobStatus.JobRunning) Nothing Nothing Nothing
            in
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , CmdUtils.sendMsg (SendWsMsg (UpdateInstanceMessage message))
            )

        StopInstance instanceId ->
            let
                message =
                    InstanceUpdate instanceId (Just JobStatus.JobStopped) Nothing Nothing Nothing
            in
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , CmdUtils.sendMsg (SendWsMsg (UpdateInstanceMessage message))
            )

        DiscardNewInstanceCreation templateId ->
            ( { oldBodyUiModel
                | expandedNewInstanceForms = resetNewParameterForm templateId oldExpandedNewInstanceForms
                , attemptedDeleteInstances = Nothing
              }
            , Cmd.none
            )

        DeleteSelectedInstances templateId selectedInstances ->
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , selectedInstances
                |> Set.toList
                |> List.map (\id -> CmdUtils.sendMsg (SendWsMsg (DeleteInstanceMessage id)))
                |> Cmd.batch
            )

        AttemptDeleteSelectedInstances templateId selectedInstances ->
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Just ( templateId, selectedInstances )
              }
            , Cmd.none
            )

        StartSelectedInstances selectedInstances ->
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , selectedInstances
                |> Set.toList
                |> List.map (\id -> CmdUtils.sendMsg (SendWsMsg (UpdateInstanceMessage (InstanceUpdate id (Just JobStatus.JobRunning) Nothing Nothing Nothing))))
                |> Cmd.batch
            )

        StopSelectedInstances selectedInstances ->
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , selectedInstances
                |> Set.toList
                |> List.map (\id -> CmdUtils.sendMsg (SendWsMsg (UpdateInstanceMessage (InstanceUpdate id (Just JobStatus.JobStopped) Nothing Nothing Nothing))))
                |> Cmd.batch
            )

        StopPeriodicJobs instanceId selectedJobs ->
            ( { oldBodyUiModel
                | attemptedDeleteInstances = Nothing
              }
            , InstanceUpdate instanceId Nothing Nothing Nothing (Just selectedJobs)
                |> UpdateInstanceMessage
                |> SendWsMsg
                |> CmdUtils.sendMsg
            )

        UpdateTemporaryStates tempStates ->
            ( { oldBodyUiModel
                | temporaryStates = tempStates
              }
            , Cmd.none
            )


expandParameterForm templateId oldExpandedNewInstanceForms maybeParameterForm =
    case maybeParameterForm of
        Just parameterForm ->
            Just parameterForm

        Nothing ->
            Just InstanceParameterForm.empty


resetEditParameterForm instanceId parameterForms =
    Dict.remove instanceId parameterForms


resetNewParameterForm templateId parameterForms =
    Dict.remove templateId parameterForms



-- TODO edit also the template somewhere (probably another function) and keep in unchanged here


addParameterValue instance parameter value maybeParameterForm =
    case maybeParameterForm of
        Just parameterForm ->
            let
                oldParameterValues =
                    parameterForm.changedParameterValues
            in
            Just
                { parameterForm | changedParameterValues = Dict.insert parameter (Just value) oldParameterValues }

        Nothing ->
            Just
                { changedParameterValues = Dict.fromList [ ( parameter, Just value ) ]
                , originalParameterValues = parameterValuesToFormValues instance.parameterValues
                , selectedTemplate = Nothing
                }


selectTemplate instance templates templateId maybeParameterForm =
    let
        selectedTemplate =
            if templateId == "" then
                Nothing

            else
                Dict.get templateId templates
    in
    case maybeParameterForm of
        Just parameterForm ->
            Just
                { parameterForm | selectedTemplate = selectedTemplate }

        Nothing ->
            Just
                { changedParameterValues = Dict.empty
                , originalParameterValues = parameterValuesToFormValues instance.parameterValues
                , selectedTemplate = selectedTemplate
                }


updateNewInstanceParameterForm parameter value maybeParameterForm =
    case maybeParameterForm of
        Just parameterForm ->
            let
                oldParameterValues =
                    parameterForm.changedParameterValues
            in
            Just
                { parameterForm | changedParameterValues = Dict.insert parameter (Just value) oldParameterValues }

        Nothing ->
            Just
                { changedParameterValues = Dict.fromList [ ( parameter, Just value ) ]
                , originalParameterValues = Dict.empty
                , selectedTemplate = Nothing
                }


parameterValuesToFormValues : Dict String (Maybe ParameterValue) -> Dict String (Maybe String)
parameterValuesToFormValues parameterValues =
    Dict.map (\k maybeVal -> maybeValueToString maybeVal) parameterValues


insertOrRemove bool insert set =
    if bool then
        Set.insert insert set

    else
        Set.remove insert set


unionOrDiff bool set1 set2 =
    if bool then
        Set.union set1 set2

    else
        Set.diff set1 set2


mapStringToParamVal parameterInfos paramName maybeValue =
    Maybe.andThen
        (\value ->
            case value of
                "" ->
                    Nothing

                _ ->
                    valueFromStringAndInfo parameterInfos paramName value |> Result.toMaybe
        )
        maybeValue
