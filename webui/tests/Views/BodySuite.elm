module Views.BodySuite exposing (tests)

import Dict exposing (Dict)
import Expect as Expect
import Maybe
import Messages
import Model exposing (TabState)
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.ClientStatus exposing (ClientStatus(..))
import Models.Resources.Instance as Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks as InstanceTasks exposing (InstanceTasks)
import Models.Resources.JobStatus exposing (JobStatus(..))
import Models.Resources.NodeResources exposing (..)
import Models.Resources.Role as Role exposing (Role(..))
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.Template as Template exposing (..)
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Models.Ui.InstanceParameterForm as InstanceParameterForm exposing (InstanceParameterForm)
import Set exposing (Set)
import Test exposing (Test, describe, test)
import Test.Html.Events as Events
import Test.Html.Query as Query
import Test.Html.Selector as Selector exposing (classes)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Views.Body as Body


tests : Test
tests =
    describe "Body View"
        [ test "Should render each template" <|
            \() ->
                Body.view defaultTabState defaultTemplates Dict.empty Dict.empty [] defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "template" ]
                    |> Query.count (Expect.equal 2)
        , test "Should render each instance" <|
            \() ->
                Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "instance-row" ]
                    |> Query.count (Expect.equal 3)
        , test "Should assign the instance to the corresponding template" <|
            \() ->
                Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.find [ Selector.id "template-t2" ]
                    |> Query.findAll [ Selector.class "instance-row" ]
                    |> Query.count (Expect.equal 2)
        , test "should render the resources view if that tab is selected" <|
            \() ->
                Body.view Model.Resources defaultTemplates defaultInstances defaultTasks [] defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.has [ Selector.id "resources-view" ]
        , test "should render the instances view if that tab is selected" <|
            \() ->
                Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] defaultBodyUiModel (Just Administrator)
                    |> Query.fromHtml
                    |> Query.has [ Selector.id "instances-view" ]
        , describe "Template Expanding"
            [ test "Expand a template on click" <|
                \() ->
                    Body.instancesView defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-template-t2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (ToggleTemplate "t2")
            , test "Render the template expansion chevron for expanded templates" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-template-t2" ]
                        |> Query.has [ Selector.class "fa-chevron-down" ]
            , test "Render the template expansion chevron for non-expanded templates" <|
                \() ->
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] defaultBodyUiModel (Just Administrator)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-new-instance-t2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (ExpandNewInstanceForm True "t2")
            , test "Should show the creation button not to operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Operator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just User)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "delete-selected-instances-t2" ]
                        |> Events.simulate Events.Click
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "confirm-delete-selected-instances-t2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (DeleteSelectedInstances "t2" (Set.fromList [ "i2" ]))
            , test "Should be disabled when no instances are selected" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Operator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just User)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "start-selected-instances-t2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (StartSelectedInstances (Set.fromList [ "i2" ]))
            , test "Should be disabled when no instances are selected" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Operator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just User)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "stop-selected-instances-t2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (StopSelectedInstances (Set.fromList [ "i2" ]))
            , test "Should be disabled when no instances are selected" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Operator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-t2" ]
                        |> Events.simulate Events.Submit
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-t2" ]
                        |> Query.findAll [ Selector.class "input-group" ]
                        |> Query.count (Expect.equal 8)
             , test "Should discard the entered parameters if clicked" <|
                \() ->
                    let
                        changedParameterValues =
                            Dict.fromList <| [ ( "i1-p1", Just "lol" ) ]
                    in
                    Body.instancesView defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-discard-button-t2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (DiscardNewInstanceCreation "t2")
             , test "Should enter parameter values correctly" <|
                \() ->
                    Body.instancesView defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-secret-visibility-t2-t2-p2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (ToggleNewInstanceSecretVisibility "t2" "t2-p2")
             , test "should render new view correctly for raw datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.RawVal "somerawvalue"

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p1" "somerawvalue"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "new-instance-form-parameter-input-error-t1-t1-p4" ]
             , test "should render new view correctly for string set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.StringVal "a"

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p5" "a"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p5" ]
                        |> Query.children [ Selector.tag "option" ]
                        |> Query.count (Expect.equal 4)
             , test "should render selected value for new view correctly for string set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.StringVal "c"

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p5" "c"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p5" ]
                        |> Query.children [ Selector.tag "option", Selector.boolAttribute "selected" True ]
                        |> Query.first
                        |> Query.has [ Selector.text <| valueToString paramVal ]
             , test "should render new view correctly for decimal set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.DecimalVal 1.1

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p6" "1.1"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p6" ]
                        |> Query.children [ Selector.tag "option" ]
                        |> Query.count (Expect.equal 4)
             , test "should render selected value for new view correctly for decimal set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.DecimalVal 2.2

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p6" "2.2"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p6" ]
                        |> Query.children [ Selector.tag "option", Selector.boolAttribute "selected" True ]
                        |> Query.first
                        |> Query.has [ Selector.text <| valueToString paramVal ]
             , test "should render new view correctly for int set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.IntVal 1

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p7" "1"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p7" ]
                        |> Query.children [ Selector.tag "option" ]
                        |> Query.count (Expect.equal 4)
             , test "should render selected value for new view correctly for int set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.IntVal 3

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p7" "3"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (changeBodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "new-instance-form-parameter-input-t1-t1-p7" ]
                        |> Query.children [ Selector.tag "option", Selector.boolAttribute "selected" True ]
                        |> Query.first
                        |> Query.has [ Selector.text <| valueToString paramVal ]
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "edit-instance-form-parameter-input-error-i1-t1-t1-p4" ]
             , test "should render edit view correctly for string set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.StringVal "a"

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p5" "a"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p5" ]
                        |> Query.children [ Selector.tag "option" ]
                        |> Query.count (Expect.equal 4)
             , test "should render selected value for edit view correctly for string set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.StringVal "c"

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p5" "c"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p5" ]
                        |> Query.children [ Selector.tag "option", Selector.boolAttribute "selected" True ]
                        |> Query.first
                        |> Query.has [ Selector.text <| valueToString paramVal ]
             , test "should render edit view correctly for decimal set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.DecimalVal 1.1

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p6" "1.1"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p6" ]
                        |> Query.children [ Selector.tag "option" ]
                        |> Query.count (Expect.equal 4)
             , test "should render selected value for edit view correctly for decimal set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.DecimalVal 2.2

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p6" "2.2"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p6" ]
                        |> Query.children [ Selector.tag "option", Selector.boolAttribute "selected" True ]
                        |> Query.first
                        |> Query.has [ Selector.text <| valueToString paramVal ]
             , test "should render edit view correctly for int set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.IntVal 1

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p7" "1"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p7" ]
                        |> Query.children [ Selector.tag "option" ]
                        |> Query.count (Expect.equal 4)
             , test "should render selected value for edit view correctly for int set datatype" <|
                \() ->
                    let
                        paramVal =
                            Template.IntVal 3

                        parameterForm =
                            changedParameterForm defaultParameterForm "t1-p7" "3"
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] (bodyUiModel parameterForm) (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "edit-instance-form-parameter-input-i1-t1-t1-p7" ]
                        |> Query.children [ Selector.tag "option", Selector.boolAttribute "selected" True ]
                        |> Query.first
                        |> Query.has [ Selector.text <| valueToString paramVal ]
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-instance-chevron-i2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (InstanceExpanded "i2" True)
            , test "Should expand on click (instance name)" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-instance-name-i2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (InstanceExpanded "i2" True)
            , test "Should select instance on check" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just User)
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
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "start-instance-i2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (StartInstance "i2")
            , test "Should stop instance on click" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.instancesView defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "stop-instance-i2" ]
                        |> Events.simulate Events.Click
                        |> Events.expectEvent (StopInstance "i2")
            , test "Should render start button for operators" <|
                \() ->
                    let
                        bodyUiModel =
                            { defaultBodyUiModel
                                | expandedTemplates = Set.fromList <| [ "t2" ]
                            }
                    in
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Operator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just Operator)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just User)
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
                    Body.view defaultTabState defaultTemplates defaultInstances defaultTasks [] bodyUiModel (Just User)
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "stop-instance-i2" ]
            ]
        , describe "Resources View"
            [ test "should show popup on mouseover" <|
                \() ->
                    let
                        bodyUiModel =
                            defaultBodyUiModel

                        temporaryStates =
                            bodyUiModel.temporaryStates

                        position =
                            ((((466 / 4594) * 100) / 2) - 5) * 0.9
                    in
                    Body.view Model.Resources defaultTemplates defaultInstances defaultTasks [ someNodeResources ] bodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find
                            [ Selector.id
                                (String.join
                                    "-"
                                    [ someNodeResources.nodeName
                                    , "CPU"
                                    , "Host"
                                    , "total"
                                    , "bg-warning"
                                    ]
                                )
                            ]
                        |> Events.simulate Events.MouseOver
                        |> Events.expectEvent
                            (Messages.UpdateBodyViewMsg
                                (UpdateTemporaryStates
                                    { temporaryStates
                                        | resourceHoverMessage =
                                            Just
                                                { nodeName = someNodeResources.nodeName
                                                , resourceType = BodyUiModel.CPU
                                                , resourceSubType = BodyUiModel.Host
                                                , resourceId = "total"
                                                , message = "466 MHz"
                                                , position = position
                                                }
                                    }
                                )
                            )
            ]
        ]


defaultBodyUiModel : BodyUiModel
defaultBodyUiModel =
    BodyUiModel.initialModel


defaultTabState : TabState
defaultTabState =
    Model.initialTabState


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
    , description = String.concat [ templateId, "-description" ]
    , version = String.concat [ templateId, "-version" ]
    , parameters =
        [ "id"
        , String.concat [ templateId, "-p1" ]
        , String.concat [ templateId, "-p2" ]
        , String.concat [ templateId, "-p3" ]
        , String.concat [ templateId, "-p4" ]
        , String.concat [ templateId, "-p5" ]
        , String.concat [ templateId, "-p6" ]
        , String.concat [ templateId, "-p7" ]
        ]
    , parameterInfos =
        [ ( String.concat [ templateId, "-p1" ]
          , { id = String.concat [ templateId, "-p1" ]
            , default = Just (Template.RawVal "default")
            , secret = Nothing
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.RawParam
            }
          )
        , ( String.concat [ templateId, "-p2" ]
          , { id = String.concat [ templateId, "-p2" ]
            , default = Just (Template.StringVal "somestring")
            , secret = Just True
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.StringParam
            }
          )
        , ( String.concat [ templateId, "-p3" ]
          , { id = String.concat [ templateId, "-p3" ]
            , default = Just (Template.IntVal 1234)
            , secret = Just True
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.IntParam
            }
          )
        , ( String.concat [ templateId, "-p4" ]
          , { id = String.concat [ templateId, "-p4" ]
            , default = Just (Template.DecimalVal 123.456)
            , secret = Just True
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.DecimalParam
            }
          )
        , ( String.concat [ templateId, "-p5" ]
          , { id = String.concat [ templateId, "-p5" ]
            , default = Nothing
            , secret = Just False
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.StringSetParam [ "a", "b", "c", "d" ]
            }
          )
        , ( String.concat [ templateId, "-p6" ]
          , { id = String.concat [ templateId, "-p6" ]
            , default = Nothing
            , secret = Just False
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.DecimalSetParam [ 1.1, 2.2, 3.3, 4.4 ]
            }
          )
        , ( String.concat [ templateId, "-p7" ]
          , { id = String.concat [ templateId, "-p7" ]
            , default = Nothing
            , secret = Just False
            , name = Nothing
            , orderIndex = Nothing
            , dataType = Template.IntSetParam [ 1, 2, 3, 4 ]
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


nodeResources =
    [ someNodeResources ]


someNodeResources : NodeResources
someNodeResources =
    { nodeId = "9a97926e-761e-4133-234b-74eca33eebaf"
    , nodeName = "nooe-02"
    , totalResources =
        { cpu = 4594
        , memoryMB = 16045
        , diskMB = 37005
        }
    , hostResources =
        { cpu = 466
        , memoryUsed = 4352520192
        , memoryTotal = 16824614912
        , diskUsed = 109628456960
        , diskSize = 135148244992
        }
    , allocatedResources =
        Dict.fromList
            [ ( "b9856be4-b6a9-98e8-d09b-4d607bd562be"
              , { id = "b9856be4-b6a9-98e8-d09b-4d607bd562be"
                , name = "nooe-az-openpage-demo.nooe-az-openpage-demo[0]"
                , cpu = 500
                , memoryMB = 2048
                , diskMB = 500
                }
              )
            , ( "12ff0f52-99d8-95b1-6da4-da11e83be1d8"
              , { id = "12ff0f52-99d8-95b1-6da4-da11e83be1d8"
                , name = "nooe-puwaf-tutorial-jupyter.nooe-puwaf-tutorial-jupyter[0]"
                , cpu = 500
                , memoryMB = 2048
                , diskMB = 500
                }
              )
            , ( "ac627019-ea4b-b926-572e-24930e7640fa"
              , { id = "ac627019-ea4b-b926-572e-24930e7640fa"
                , name = "nooe-ml-demo.nooe-ml-demo[0]"
                , cpu = 500
                , memoryMB = 2048
                , diskMB = 500
                }
              )
            , ( "e395a449-1929-ca71-58c6-cbcd5edf27fe"
              , { id = "e395a449-1929-ca71-58c6-cbcd5edf27fe"
                , name = "nooe-training-zeppelin.zeppelin[0]"
                , cpu = 500
                , memoryMB = 2048
                , diskMB = 500
                }
              )
            ]
    , allocatedResourcesUtilization =
        Dict.fromList
            [ ( "b9856be4-b6a9-98e8-d09b-4d607bd562be"
              , { id = "b9856be4-b6a9-98e8-d09b-4d607bd562be"
                , name = "nooe-az-openpage-demo.nooe-az-openpage-demo[0]"
                , cpu = 1
                , memory = 139378688
                }
              )
            , ( "12ff0f52-99d8-95b1-6da4-da11e83be1d8"
              , { id = "12ff0f52-99d8-95b1-6da4-da11e83be1d8"
                , name = "nooe-puwaf-tutorial-jupyter.nooe-puwaf-tutorial-jupyter[0]"
                , cpu = 2
                , memory = 178147328
                }
              )
            , ( "ac627019-ea4b-b926-572e-24930e7640fa"
              , { id = "ac627019-ea4b-b926-572e-24930e7640fa"
                , name = "nooe-ml-demo.nooe-ml-demo[0]"
                , cpu = 1
                , memory = 108736512
                }
              )
            , ( "e395a449-1929-ca71-58c6-cbcd5edf27fe"
              , { id = "e395a449-1929-ca71-58c6-cbcd5edf27fe"
                , name = "nooe-training-zeppelin.zeppelin[0]"
                , cpu = 18
                , memory = 1506852864
                }
              )
            ]
    , totalAllocated =
        { cpu = 3100
        , memoryMB = 13824
        , diskMB = 4824
        }
    , totalUtilized =
        { cpu = 28
        , memory = 2444763136
        }
    }
