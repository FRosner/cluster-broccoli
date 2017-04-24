module Main exposing (main)

import Html exposing (..)
import Html.Attributes exposing (..)
import Set exposing (Set)
import Models.Resources.Template exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.ServiceStatus exposing (..)
import Models.Resources.JobStatus exposing (..)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Resources.PeriodicRun as PeriodicRun exposing (PeriodicRun)
import Models.Ui.BodyUiModel as BodyUiModel exposing (BodyUiModel)
import Models.Ui.LoginForm exposing (..)
import Models.Ui.Notifications exposing (..)
import Updates.UpdateAboutInfo exposing (updateAboutInfo)
import Updates.UpdateErrors exposing (updateErrors)
import Updates.UpdateLoginForm exposing (updateLoginForm)
import Updates.UpdateLoginStatus exposing (updateLoginStatus)
import Updates.UpdateBodyView exposing (updateBodyView)
import Updates.UpdateTemplates exposing (updateTemplates)
import Updates.Messages exposing (UpdateAboutInfoMsg(..), UpdateLoginStatusMsg(..), UpdateErrorsMsg(..), UpdateTemplatesMsg(..))
import Messages exposing (AnyMsg(..))
import Views.Header
import Views.Body
import Views.Footer
import Views.Notifications

import Ws
import WebSocket
import Dict exposing (Dict)

-- TODO what type of submessages do I want to have?
-- - Messages changing resources
-- - Error messages
-- - Messages changing the view
-- so one message per entry in my model? that means that not every single thing should define its own Msg type otherwise it will get crazy

type alias Model =
  { aboutInfo : Maybe AboutInfo
  , errors : Errors
  , loginForm : LoginForm
  , loggedIn : Maybe UserInfo
  , authEnabled : Maybe Bool
  , instances : List Instance
  , templates : List Template
  , bodyUiModel : BodyUiModel
  }


initialModel : Model
initialModel =
  { aboutInfo = Nothing
  -- , templates = []
  , errors = []
  , loginForm = emptyLoginForm
  , loggedIn = Nothing
  , authEnabled = Nothing
  , bodyUiModel = BodyUiModel.initialModel
  , templates = []
  , instances = []
  }

init : ( Model, Cmd AnyMsg )
init =
  ( initialModel
  , Cmd.none
  )

update : AnyMsg -> Model -> ( Model, Cmd AnyMsg )
update msg model =
  case msg of
    ProcessWsMsg wsMsg ->
      Ws.update wsMsg model
    SendWsMsg jsonObject wsMsgType ->
      ( model
      , Ws.send jsonObject wsMsgType
      )
    UpdateAboutInfoMsg subMsg ->
      let (newAbout, cmd) =
        updateAboutInfo subMsg model.aboutInfo
      in
        ( { model | aboutInfo = newAbout }
        , cmd
        )
    UpdateLoginStatusMsg subMsg ->
      let (newLoginStatus, cmd) =
        updateLoginStatus subMsg model.loggedIn
      in
        ({ model | loggedIn = newLoginStatus }
        , cmd
        )
    UpdateErrorsMsg subMsg ->
      let (newErrors, cmd) =
        updateErrors subMsg model.errors
      in
        ({ model | errors = newErrors }
        , cmd
        )
    UpdateBodyViewMsg subMsg ->
      let (newBodyUiModel, cmd) =
        updateBodyView subMsg model.bodyUiModel
      in
        ( { model | bodyUiModel = newBodyUiModel }
        , cmd
        )
    UpdateTemplatesMsg subMsg ->
      let (newTemplates, cmd) =
        updateTemplates subMsg model.templates
      in
        ( { model | templates = newTemplates }
        , cmd
        )
    UpdateLoginFormMsg subMsg ->
      let (newLoginForm, cmd) =
        updateLoginForm subMsg model.loginForm
      in
        ({ model | loginForm = newLoginForm }
        , cmd
        )
    NoOp -> (model, Cmd.none)

view : Model -> Html AnyMsg
view model =
  div
    []
    [ Views.Header.view model.aboutInfo model.loginForm model.loggedIn model.authEnabled
    , Views.Notifications.view model.errors
    , Html.map
        UpdateBodyViewMsg
        ( Views.Body.view
            model.templates
            model.instances
            model.bodyUiModel
        )
    , text (toString model)
    , Views.Footer.view model.aboutInfo
    ]

-- Sub.map UpdateErrorsMsg ( WebSocket.listen "ws://localhost:9000/ws" AddError ) when AddError is an UpdateErrorsMsg
subscriptions : Model -> Sub AnyMsg
subscriptions model =
  -- TODO cut the websocket connection on logout
  Ws.listen

main =
  program
    { init = init
    , view = view
    , update = update
    , subscriptions = subscriptions
    }
