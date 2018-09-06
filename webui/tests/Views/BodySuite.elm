module Views.BodySuite exposing (tests)

import Views.Body as Body
import Models.Resources.Role as Role exposing (Role(..))
import Models.Resources.Template as Template exposing (..)
import Models.Resources.Instance as Instance exposing (Instance, InstanceId)
import Models.Resources.JobStatus exposing (JobStatus(..))
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.InstanceTasks as InstanceTasks exposing (InstanceTasks)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.ClientStatus exposing (ClientStatus(..))
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Test exposing (test, describe, Test)
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Test.Html.Events as Events
import Expect as Expect
import Dict exposing (Dict)
import Set exposing (Set)
import Maybe


tests : Test
tests =
    describe "Body View"
        [ test "Should render each template" <|
            \() ->
                Body.view defaultTemplates Dict.empty Dict.empty defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "template" ]
                    |> Query.count (Expect.equal 2)
        , test "Should render each instance" <|
            \() ->
                Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "instance-row" ]
                    |> Query.count (Expect.equal 3)
        , test "Should assign the instance to the corresponding template" <|
            \() ->
                Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.find [ Selector.id "template-t2" ]
                    |> Query.findAll [ Selector.class "instance-row" ]
                    |> Query.count (Expect.equal 2)
        , describe "Template Expanding"
            [ test "Expand a template on click" <|
                \() ->
                    Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-template-t2" ]
                        |> Events.simulate (Events.Click)
                        |> Events.expectEvent (ToggleTemplate "t2")
            , test "Render the template expansion chevron for expanded templates" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "expand-template-t2" ]
                            |> Query.has [ Selector.class "fa-chevron-down" ]
            , test "Render the template expansion chevron for non-expanded templates" <|
                \() ->
                    Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-template-t2" ]
                        |> Query.has [ Selector.class "fa-chevron-right" ]
            ]
        , describe "Instance Creation"
            [ test "Should open the instance creation form on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "expand-new-instance-t2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (ExpandNewInstanceForm True "t2")
            , test "Should show the creation button not to operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Operator)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "expand-new-instance-t2" ]
            , test "Should show the creation button not to users" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "expand-new-instance-t2" ]
            ]
        , describe "Selected Instance Deletion"
            [ test "Should trigger instance deletion confirmation on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                                , selectedInstances = Set.fromList <| [ "i1", "i2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "delete-selected-instances-t2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (AttemptDeleteSelectedInstances "t2" (Set.fromList [ "i2" ]))
            , test "Should trigger instance deletion on confirmation" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                                , selectedInstances = Set.fromList <| [ "i1", "i2" ]
                                , attemptedDeleteInstances = Just ( "t2", Set.fromList <| [ "i2" ] )
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "confirm-delete-selected-instances-t2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (DeleteSelectedInstances "t2" (Set.fromList [ "i2" ]))
            , test "Should be disabled when no instances are selected" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "delete-selected-instances-t2" ]
                            |> Query.has [ Selector.attribute "disabled" "disabled" ]
            , test "Should show the deletion button not to operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Operator)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "delete-selected-instances-t2" ]
            , test "Should show the deletion button not to users" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "delete-selected-instances-t2" ]
            ]
        , describe "Selected Instance Starting"
            [ test "Should trigger instance start on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                                , selectedInstances = Set.fromList <| [ "i1", "i2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "start-selected-instances-t2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (StartSelectedInstances (Set.fromList [ "i2" ]))
            , test "Should be disabled when no instances are selected" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "start-selected-instances-t2" ]
                            |> Query.has [ Selector.attribute "disabled" "disabled" ]
            , test "Should show the start button to operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Operator)
                            |> Query.fromHtml
                            |> Query.has [ Selector.id "start-selected-instances-t2" ]
            , test "Should show the start button not to users" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "start-selected-instances-t2" ]
            ]
        , describe "Selected Instance Stopping"
            [ test "Should trigger instance stop on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                                , selectedInstances = Set.fromList <| [ "i1", "i2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "stop-selected-instances-t2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (StopSelectedInstances (Set.fromList [ "i2" ]))
            , test "Should be disabled when no instances are selected" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "stop-selected-instances-t2" ]
                            |> Query.has [ Selector.attribute "disabled" "disabled" ]
            , test "Should show the stop button to operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Operator)
                            |> Query.fromHtml
                            |> Query.has [ Selector.id "stop-selected-instances-t2" ]
            , test "Should show the stop button not to users" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "stop-selected-instances-t2" ]
            ]
        , describe "New Instance Form"
            (let
                changeBodyUiModel parameterForm =
                    { defaultBodyUiModel
                        | expandedTemplates = Set.fromList <| [ "t1" ]
                        , expandedNewInstanceForms = Dict.fromList <| [ ( "t1", parameterForm ) ]
                    }
             in
                [ test "Should be invisible if it is not expanded" <|
                    \() ->
                        let
                            bodyUiModel =
                                { defaultBodyUiModel
                                    | expandedTemplates = Set.fromList <| [ "t2" ]
                                }
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-container-t2" ]
                                |> Query.has [ Selector.class "d-none" ]
                , test "Should be visible if it is expanded" <|
                    \() ->
                        let
                            bodyUiModel =
                                { defaultBodyUiModel
                                    | expandedTemplates = Set.fromList <| [ "t2" ]
                                    , expandedNewInstanceForms =
                                        Dict.fromList <|
                                            [ ( "t2"
                                              , { originalParameterValues = Dict.empty
                                                , changedParameterValues = Dict.empty
                                                , selectedTemplate = Nothing
                                                }
                                              )
                                            ]
                                }
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-container-t2" ]
                                |> Query.has [ Selector.class "show" ]
                , test "Should submit the entered parameters correctly" <|
                    \() ->
                        let
                            changedParameterValues =
                                Dict.fromList <| [ ( "i1-p1", Just "lol" ) ]
                        in
                            let
                                bodyUiModel =
                                    { defaultBodyUiModel
                                        | expandedTemplates = Set.fromList <| [ "t2" ]
                                        , expandedNewInstanceForms =
                                            Dict.fromList <|
                                                [ ( "t2"
                                                  , { originalParameterValues = Dict.empty
                                                    , changedParameterValues = changedParameterValues
                                                    , selectedTemplate = Nothing
                                                    }
                                                  )
                                                ]
                                    }

                                paramInfos =
                                    Dict.get "t2" defaultTemplates
                                        |> Maybe.andThen (\t -> Just t.parameterInfos)
                                        |> Maybe.withDefault Dict.empty
                            in
                                Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                                    |> Query.fromHtml
                                    |> Query.find [ Selector.id "new-instance-form-t2" ]
                                    |> Events.simulate (Events.Submit)
                                    |> Events.expectEvent (SubmitNewInstanceCreation "t2" paramInfos changedParameterValues)
                , test "Should render input groups for all parameters" <|
                    \() ->
                        let
                            bodyUiModel =
                                { defaultBodyUiModel
                                    | expandedTemplates = Set.fromList <| [ "t2" ]
                                    , expandedNewInstanceForms =
                                        Dict.fromList <|
                                            [ ( "t2"
                                              , { originalParameterValues = Dict.empty
                                                , changedParameterValues = Dict.empty
                                                , selectedTemplate = Nothing
                                                }
                                              )
                                            ]
                                }
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-t2" ]
                                |> Query.findAll [ Selector.class "input-group" ]
                                |> Query.count (Expect.equal 5)
                , test "Should discard the entered parameters if clicked" <|
                    \() ->
                        let
                            changedParameterValues =
                                Dict.fromList <| [ ( "i1-p1", Just "lol" ) ]
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-discard-button-t2" ]
                                |> Events.simulate (Events.Click)
                                |> Events.expectEvent (DiscardNewInstanceCreation "t2")
                , test "Should enter parameter values correctly" <|
                    \() ->
                        Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "new-instance-form-parameter-input-t2-t2-p1" ]
                            |> Events.simulate (Events.Input "value")
                            |> Events.expectEvent (EnterNewInstanceParameterValue "t2" "t2-p1" "value")
                , test "Should toggle secret parameter visibility" <|
                    \() ->
                        let
                            bodyUiModel =
                                { defaultBodyUiModel
                                    | expandedTemplates = Set.fromList <| [ "t2" ]
                                    , expandedNewInstanceForms =
                                        Dict.fromList <|
                                            [ ( "t2"
                                              , { originalParameterValues = Dict.empty
                                                , changedParameterValues = Dict.empty
                                                , selectedTemplate = Nothing
                                                }
                                              )
                                            ]
                                }
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-parameter-secret-visibility-t2-t2-p2" ]
                                |> Events.simulate (Events.Click)
                                |> Events.expectEvent (ToggleNewInstanceSecretVisibility "t2" "t2-p2")
                , test "should render new view correctly for raw datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.RawVal "somerawvalue"

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p1" "somerawvalue"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (changeBodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                -- i1 is the instanceId, t1 the templateId and t1-p1 the parameterName
                                |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p1" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should render new view correctly for string datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.StringVal "somestringvalue"

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p2" "somestringvalue"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (changeBodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p2" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should render new view correctly for integer datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.IntVal 4567

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p3" "4567"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (changeBodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p3" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should render new view correctly for decimal datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.DecimalVal 678.93

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p4" "678.93"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (changeBodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p4" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should show an error for wrong input from user for a integer field in new view" <|
                    \() ->
                        let
                            paramVal =
                                Template.IntVal 678

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p3" "678a"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (changeBodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.has [ Selector.id "new-instance-form-parameter-input-error-t1-t1-p3" ]
                , test "should show an error for wrong input from user for a decimal field in new view" <|
                    \() ->
                        let
                            paramVal =
                                Template.DecimalVal 678.93

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p4" "678.93a"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (changeBodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.has [ Selector.id "new-instance-form-parameter-input-error-t1-t1-p4" ]
                ]
            )
        , describe "Edit Instance Form"
            (let
                bodyUiModel parameterForm =
                    { defaultBodyUiModel
                        | expandedTemplates = Set.fromList <| [ "t1" ]
                        , expandedInstances = Set.fromList <| [ "i1" ]
                        , instanceParameterForms = Dict.fromList <| [ ( "i1", parameterForm ) ]
                    }
             in
                [ test "should render edit view correctly for raw datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.RawVal "somerawvalue"

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p1" "somerawvalue"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (bodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                -- i1 is the instanceId, t1 the templateId and t1-p1 the parameterName
                                |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p1" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should render edit view correctly for string datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.StringVal "somestringvalue"

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p2" "somestringvalue"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (bodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p2" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should render edit view correctly for integer datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.IntVal 4567

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p3" "4567"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (bodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p3" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should render edit view correctly for decimal datatype" <|
                    \() ->
                        let
                            paramVal =
                                Template.DecimalVal 678.93

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p4" "678.93"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (bodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p4" ]
                                |> Query.has [ Selector.attribute "value" (valueToString paramVal) ]
                , test "should show an error for wrong input from user for a integer field in edit view" <|
                    \() ->
                        let
                            paramVal =
                                Template.IntVal 678

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p3" "678a"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (bodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.has [ Selector.id "edit-instance-form-parameter-input-error-i1-t1-t1-p3" ]
                , test "should show an error for wrong input from user for a decimal field in edit view" <|
                    \() ->
                        let
                            paramVal =
                                Template.DecimalVal 678.93

                            parameterForm =
                                changedParameterForm defaultParameterForm "t1-p4" "678.93a"
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks (bodyUiModel parameterForm) (Just User)
                                |> Query.fromHtml
                                |> Query.has [ Selector.id "edit-instance-form-parameter-input-error-i1-t1-t1-p4" ]
                ]
            )
        , describe "Instance View"
            [ test "Should expand on click (chevron)" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "expand-instance-chevron-i2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (InstanceExpanded "i2" True)
            , test "Should expand on click (instance name)" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "expand-instance-name-i2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (InstanceExpanded "i2" True)
            , test "Should select instance on check" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "select-instance-i2" ]
                            |> Events.simulate (Events.Check True)
                            |> Events.expectEvent (InstanceSelected "i2" True)
            , test "Should start instance on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "start-instance-i2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (StartInstance "i2")
            , test "Should stop instance on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                            |> Query.fromHtml
                            |> Query.find [ Selector.id "stop-instance-i2" ]
                            |> Events.simulate (Events.Click)
                            |> Events.expectEvent (StopInstance "i2")
            , test "Should render start button for operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Operator)
                            |> Query.fromHtml
                            |> Query.has [ Selector.id "start-instance-i2" ]
            , test "Should render stop button for operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Operator)
                            |> Query.fromHtml
                            |> Query.has [ Selector.id "stop-instance-i2" ]
            , test "Should not render start button for users" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "start-instance-i2" ]
            , test "Should not render start button for users" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                        Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                            |> Query.fromHtml
                            |> Query.hasNot [ Selector.id "stop-instance-i2" ]
            ]
        ]


defaultBodyUiModel : BodyUiModel
defaultBodyUiModel =
    BodyUiModel.initialModel


defaultParameterForm : InstanceParameterForm
defaultParameterForm =
    InstanceParameterForm.empty


changedParameterForm : InstanceParameterForm -> String -> String -> InstanceParameterForm
changedParameterForm originalParameterForm paramName maybeParamVal =
    { originalParameterForm
        | changedParameterValues =
            Dict.update
                paramName
                (always (Just (Just maybeParamVal)))
                originalParameterForm.changedParameterValues
    }


defaultInstance : InstanceId -> TemplateId -> Instance
defaultInstance instanceId templateId =
    { id = instanceId
    , template = defaultTemplate templateId
    , parameterValues = Dict.empty
    , jobStatus = JobStopped
    , services = []
    , periodicRuns = []
    }


defaultInstances : Dict InstanceId Instance
defaultInstances =
    [ ( "i1", defaultInstance "i1" "t1" )
    , ( "i2", defaultInstance "i2" "t2" )
    , ( "i3", defaultInstance "i3" "t2" )
    ]
        |> Dict.fromList


defaultTasks : Dict InstanceId InstanceTasks
defaultTasks =
    [ ( "i1", InstanceTasks.empty "i1" )
    , ( "i2"
      , { instanceId = "i2"
        , allocatedTasks = [ defaultTask ]
        , allocatedPeriodicTasks =
            [ ( "i2/periodic-1234", [ defaultTask ] ) ]
                |> Dict.fromList
        }
      )
    ]
        |> Dict.fromList


defaultTask : AllocatedTask
defaultTask =
    { taskName = "t1"
    , taskState = TaskRunning
    , allocationId = "a1"
    , clientStatus = ClientRunning
    , resources =
        { cpuRequiredMhz = Nothing
        , cpuUsedMhz = Nothing
        , memoryRequiredBytes = Nothing
        , memoryUsedBytes = Nothing
        }
    }


defaultTemplate : TemplateId -> Template
defaultTemplate templateId =
    { id = templateId
    , description = (String.concat [ templateId, "-description" ])
    , version = (String.concat [ templateId, "-version" ])
    , parameters =
        [ "id"
        , (String.concat [ templateId, "-p1" ])
        , (String.concat [ templateId, "-p2" ])
        , (String.concat [ templateId, "-p3" ])
        , (String.concat [ templateId, "-p4" ])
        ]
    , parameterInfos =
        [ ( (String.concat [ templateId, "-p1" ])
          , { id = (String.concat [ templateId, "-p1" ])
            , default = Just (Template.RawVal "default")
            , secret = Nothing
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.RawParam
            }
          )
        , ( (String.concat [ templateId, "-p2" ])
          , { id = (String.concat [ templateId, "-p2" ])
            , default = Just (Template.StringVal "somestring")
            , secret = Just True
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.StringParam
            }
          )
        , ( (String.concat [ templateId, "-p3" ])
          , { id = (String.concat [ templateId, "-p3" ])
            , default = Just (Template.IntVal 1234)
            , secret = Just True
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.IntParam
            }
          )
        , ( (String.concat [ templateId, "-p4" ])
          , { id = (String.concat [ templateId, "-p4" ])
            , default = Just (Template.DecimalVal 123.456)
            , secret = Just True
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.DecimalParam
            }
          )
        ]
            |> Dict.fromList
    }


defaultTemplates : Dict TemplateId Template
defaultTemplates =
    [ ( "t1", defaultTemplate "t1" )
    , ( "t2", defaultTemplate "t2" )
    ]
        |> Dict.fromList
