module Views.BodySuite exposing (tests)

import Views.Body as Body
import Models.Resources.Role as Role exposing (Role(..))
import Models.Resources.Template as Template exposing (Template, TemplateId)
import Models.Resources.Instance as Instance exposing (Instance, InstanceId)
import Models.Resources.JobStatus exposing (JobStatus(..))
import Models.Resources.Task exposing (Task)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Test exposing (test, describe, Test)
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Test.Html.Events as Events
import Expect as Expect
import Dict exposing (Dict)
import Set exposing (Set)


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
                            |> Query.has [ Selector.class "glyphicon-chevron-down" ]
            , test "Render the template expansion chevron for non-expanded templates" <|
                \() ->
                    Body.view defaultTemplates defaultInstances defaultTasks defaultBodyUiModel (Just Administrator)
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "expand-template-t2" ]
                        |> Query.has [ Selector.class "glyphicon-chevron-right" ]
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
                            |> Query.has [ Selector.class "hidden" ]
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
                        in
                            Body.view defaultTemplates defaultInstances defaultTasks bodyUiModel (Just Administrator)
                                |> Query.fromHtml
                                |> Query.find [ Selector.id "new-instance-form-t2" ]
                                |> Events.simulate (Events.Submit)
                                |> Events.expectEvent (SubmitNewInstanceCreation "t2" changedParameterValues)
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
                            |> Query.count (Expect.equal 3)
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
            ]
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


defaultTasks : Dict InstanceId (List Task)
defaultTasks =
    [ ( "i1", [] )
    , ( "i2"
      , [ { name = "t1"
          , allocations =
                [ { id = "a1", state = TaskRunning }
                ]
          }
        ]
      )
    ]
        |> Dict.fromList


defaultTemplate : TemplateId -> Template
defaultTemplate templateId =
    { id = templateId
    , description = (String.concat [ templateId, "-description" ])
    , version = (String.concat [ templateId, "-version" ])
    , parameters =
        [ "id"
        , (String.concat [ templateId, "-p1" ])
        , (String.concat [ templateId, "-p2" ])
        ]
    , parameterInfos =
        [ ( (String.concat [ templateId, "-p1" ])
          , { id = (String.concat [ templateId, "-p1" ])
            , default = Just "default"
            , secret = Nothing
            , name = Nothing
            }
          )
        , ( (String.concat [ templateId, "-p2" ])
          , { id = (String.concat [ templateId, "-p2" ])
            , default = Nothing
            , secret = Just True
            , name = Nothing
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
