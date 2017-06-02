module Views.BodySuite exposing (tests)

import Views.Body as Body

import Models.Resources.Role as Role exposing (Role(..))
import Models.Resources.Template as Template exposing (Template, TemplateId)
import Models.Resources.Instance as Instance exposing (Instance, InstanceId)
import Models.Resources.JobStatus exposing (JobStatus(..))
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)

import Updates.Messages exposing (UpdateBodyViewMsg(..))

import Messages exposing (AnyMsg(UpdateBodyViewMsg))

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
          let
            templates = defaultTemplates
            instances = Dict.empty
            bodyUiModel = defaultBodyUiModel
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.findAll [ Selector.class "template" ]
            |> Query.count (Expect.equal 2)

    , test "Should render each instance" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel = defaultBodyUiModel
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.findAll [ Selector.class "instance-row" ]
            |> Query.count (Expect.equal 3)

      , test "Should assign the instance to the corresponding template" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel = defaultBodyUiModel
              maybeRole = Just Administrator
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.find [ Selector.id "template-t2" ]
              |> Query.findAll [ Selector.class "instance-row" ]
              |> Query.count (Expect.equal 2)

        , describe "Template Expanding"

          [ test "Expand a template on click" <|
            \() ->
              let
                templates = defaultTemplates
                instances = defaultInstances
                bodyUiModel = defaultBodyUiModel
                maybeRole = Just Administrator
              in
                Body.view templates instances bodyUiModel maybeRole
                |> Query.fromHtml
                |> Query.find [ Selector.id "expand-template-t2" ]
                |> Events.simulate (Events.Click)
                |> Events.expectEvent (ToggleTemplate "t2")

          , test "Render the template expansion chevron for expanded templates" <|
            \() ->
              let
                templates = defaultTemplates
                instances = defaultInstances
                bodyUiModel =
                  { defaultBodyUiModel
                  | expandedTemplates = Set.fromList <| [ "t2" ]
                  }
                maybeRole = Just Administrator
              in
                Body.view templates instances bodyUiModel maybeRole
                |> Query.fromHtml
                |> Query.find [ Selector.id "expand-template-t2" ]
                |> Query.has [ Selector.class "glyphicon-chevron-down" ]

          , test "Render the template expansion chevron for non-expanded templates" <|
            \() ->
              let
                templates = defaultTemplates
                instances = defaultInstances
                bodyUiModel = defaultBodyUiModel
                maybeRole = Just Administrator
              in
                Body.view templates instances bodyUiModel maybeRole
                |> Query.fromHtml
                |> Query.find [ Selector.id "expand-template-t2" ]
                |> Query.has [ Selector.class "glyphicon-chevron-right" ]
          ]

      , describe "Instance Creation"

        [ test "Should open the instance creation form on click" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                }
              maybeRole = Just Administrator
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.find [ Selector.id "expand-new-instance-t2" ]
              |> Events.simulate (Events.Click)
              |> Events.expectEvent (ExpandNewInstanceForm True "t2")

        , test "Should show the creation button not to operators" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                }
              maybeRole = Just Operator
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "expand-new-instance-t2" ]

        , test "Should show the creation button not to users" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                }
              maybeRole = Just User
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "expand-new-instance-t2" ]

        ]

    , describe "Selected Instance Deletion"

      [ test "Should trigger instance deletion confirmation on click" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              , selectedInstances = Set.fromList <| [ "i1", "i2" ]
              }
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.find [ Selector.id "delete-selected-instances-t2" ]
            |> Events.simulate (Events.Click)
            |> Events.expectEvent (AttemptDeleteSelectedInstances "t2" (Set.fromList ["i2"]))

      , test "Should trigger instance deletion on confirmation" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              , selectedInstances = Set.fromList <| [ "i1", "i2" ]
              , attemptedDeleteInstances = Just ("t2", Set.fromList <| [ "i2" ])
              }
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.find [ Selector.id "confirm-delete-selected-instances-t2" ]
            |> Events.simulate (Events.Click)
            |> Events.expectEvent (DeleteSelectedInstances "t2" (Set.fromList ["i2"]))

      , test "Should be disabled when no instances are selected" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              }
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.find [ Selector.id "delete-selected-instances-t2" ]
            |> Query.has [ Selector.attribute "disabled" "disabled" ]

      , test "Should show the deletion button not to operators" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              }
            maybeRole = Just Operator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.hasNot [ Selector.id "delete-selected-instances-t2" ]

      , test "Should show the deletion button not to users" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              }
            maybeRole = Just User
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.hasNot [ Selector.id "delete-selected-instances-t2" ]

      ]

    , describe "Selected Instance Starting"

      [ test "Should trigger instance start on click" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              , selectedInstances = Set.fromList <| [ "i1", "i2" ]
              }
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.find [ Selector.id "start-selected-instances-t2" ]
            |> Events.simulate (Events.Click)
            |> Events.expectEvent (StartSelectedInstances (Set.fromList ["i2"]))

      , test "Should be disabled when no instances are selected" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              }
            maybeRole = Just Administrator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.find [ Selector.id "start-selected-instances-t2" ]
            |> Query.has [ Selector.attribute "disabled" "disabled" ]

      , test "Should show the start button to operators" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              }
            maybeRole = Just Operator
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.has [ Selector.id "start-selected-instances-t2" ]

      , test "Should show the start button not to users" <|
        \() ->
          let
            templates = defaultTemplates
            instances = defaultInstances
            bodyUiModel =
              { defaultBodyUiModel
              | expandedTemplates = Set.fromList <| [ "t2" ]
              }
            maybeRole = Just User
          in
            Body.view templates instances bodyUiModel maybeRole
            |> Query.fromHtml
            |> Query.hasNot [ Selector.id "start-selected-instances-t2" ]

      ]

      , describe "Selected Instance Stopping"

        [ test "Should trigger instance stop on click" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                , selectedInstances = Set.fromList <| [ "i1", "i2" ]
                }
              maybeRole = Just Administrator
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.find [ Selector.id "stop-selected-instances-t2" ]
              |> Events.simulate (Events.Click)
              |> Events.expectEvent (StopSelectedInstances (Set.fromList ["i2"]))

        , test "Should be disabled when no instances are selected" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                }
              maybeRole = Just Administrator
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.find [ Selector.id "stop-selected-instances-t2" ]
              |> Query.has [ Selector.attribute "disabled" "disabled" ]

        , test "Should show the stop button to operators" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                }
              maybeRole = Just Operator
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.has [ Selector.id "stop-selected-instances-t2" ]

        , test "Should show the stop button not to users" <|
          \() ->
            let
              templates = defaultTemplates
              instances = defaultInstances
              bodyUiModel =
                { defaultBodyUiModel
                | expandedTemplates = Set.fromList <| [ "t2" ]
                }
              maybeRole = Just User
            in
              Body.view templates instances bodyUiModel maybeRole
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "stop-selected-instances-t2" ]

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

defaultTemplate : TemplateId -> Template
defaultTemplate templateId =
  { id = templateId
  , description = ( String.concat [ templateId, "-description" ] )
  , version = ( String.concat [ templateId, "-version" ] )
  , parameters =
     [ "id"
     , ( String.concat [ templateId, "-p1" ] )
     , ( String.concat [ templateId, "-p2" ] )
     ]
  , parameterInfos =
      [ ( ( String.concat [ templateId, "-p1" ] )
        , { name = ( String.concat [ templateId, "-p1" ] )
          , default = Just "default"
          , secret = Nothing
          }
        )
      , ( ( String.concat [ templateId, "-p2" ] )
        , { name = ( String.concat [ templateId, "-p2" ] )
          , default = Nothing
          , secret = Just True
          }
        )
      ]
      |> Dict.fromList
  }

defaultTemplates : Dict TemplateId Template
defaultTemplates =
  [ ("t1", defaultTemplate "t1")
  , ("t2", defaultTemplate "t2")
  ]
  |> Dict.fromList
