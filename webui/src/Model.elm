module Model exposing (Model, Route(..), TabState(..), initial, initialTabState)

import Dict exposing (Dict)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.NodeResources exposing (NodeResources)
import Models.Resources.Template exposing (Template, TemplateId)
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Models.Ui.LoginForm as LoginForm exposing (LoginForm)
import Models.Ui.Notifications exposing (Errors)
import Navigation exposing (Location)


type Route
    = MainRoute


type TabState
    = Instances
    | Resources


initialTabState =
    Instances


{-| The application state model

This model holds the entire state of the whole application.

-}
type alias Model =
    { tabState : TabState
    , aboutInfo : Maybe AboutInfo
    , errors : Errors
    , loginForm : LoginForm
    , authRequired : Maybe Bool
    , instances : Dict InstanceId Instance
    , tasks : Dict InstanceId InstanceTasks
    , templates : Dict TemplateId Template
    , nodesResources : List NodeResources
    , bodyUiModel : BodyUiModel
    , wsConnected : Bool
    , route : Route
    , location : Location
    , templateFilter : String
    , instanceFilter : String
    , nodeFilter : String
    }


{-| Initial mostly empty values for the application state
-}
initial : Location -> Route -> Model
initial location route =
    { tabState = initialTabState
    , aboutInfo = Nothing
    , errors = []
    , loginForm = LoginForm.empty
    , authRequired = Nothing
    , bodyUiModel = BodyUiModel.initialModel
    , templates = Dict.empty
    , nodesResources = []
    , tasks = Dict.empty
    , instances = Dict.empty
    , wsConnected = False
    , route = route
    , location = location
    , templateFilter = ""
    , instanceFilter = ""
    , nodeFilter = ""
    }
