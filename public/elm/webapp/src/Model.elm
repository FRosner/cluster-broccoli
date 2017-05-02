module Model exposing (Model, initial)

import Models.Resources.Template exposing (Template)
import Models.Resources.Instance exposing (Instance)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Models.Ui.LoginForm as LoginForm exposing (LoginForm)
import Models.Ui.Notifications exposing (Errors)

type alias Model =
  { aboutInfo : Maybe AboutInfo
  , errors : Errors
  , loginForm : LoginForm
  , loggedIn : Maybe UserInfo
  , authEnabled : Maybe Bool
  , instances : List Instance
  , templates : List Template
  , bodyUiModel : BodyUiModel
  , wsConnected : Bool
  }

initial : Model
initial =
  { aboutInfo = Nothing
  , errors = []
  , loginForm = LoginForm.empty
  , loggedIn = Nothing
  , authEnabled = Nothing
  , bodyUiModel = BodyUiModel.initialModel
  , templates = []
  , instances = []
  , wsConnected = False
  }
