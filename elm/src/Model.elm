module Model exposing (Model, Route(..), initial)

import Models.Resources.Template exposing (Template, TemplateId)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Models.Ui.LoginForm as LoginForm exposing (LoginForm)
import Models.Ui.Notifications exposing (Errors)
import Navigation exposing (Location)
import Regex exposing (Regex)
import Dict exposing (Dict)


type Route
    = MainRoute


type alias Model =
    { aboutInfo : Maybe AboutInfo
    , errors : Errors
    , loginForm : LoginForm
    , authRequired : Maybe Bool
    , instances : Dict InstanceId Instance
    , templates : Dict TemplateId Template
    , bodyUiModel : BodyUiModel
    , wsConnected : Bool
    , route : Route
    , location : Location
    , templateFilter : String
    , instanceFilter : String
    }


initial : Location -> Route -> Model
initial location route =
    { aboutInfo = Nothing
    , errors = []
    , loginForm = LoginForm.empty
    , authRequired = Nothing
    , bodyUiModel = BodyUiModel.initialModel
    , templates = Dict.empty
    , instances = Dict.empty
    , wsConnected = False
    , route = route
    , location = location
    , templateFilter = ""
    , instanceFilter = ""
    }
