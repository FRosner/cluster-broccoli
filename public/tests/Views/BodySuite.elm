module Views.BodySuite exposing (tests)

import Views.Body as Body

import Models.Resources.Role as Role exposing (Role(Administrator))
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

        , test "Expand a template on click" <|
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
