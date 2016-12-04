module Main exposing (main)

import Html exposing (..)
import Html.Attributes exposing (..)
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId, Template)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Updates.UpdateAboutInfo exposing (updateAboutInfo)
import Updates.UpdateErrors exposing (updateErrors)
import Updates.UpdateLoginForm exposing (updateLoginForm)
import Updates.UpdateLoginStatus exposing (updateLoginStatus)
import Updates.Messages exposing (UpdateAboutInfoMsg, UpdateLoginStatusMsg)
import Commands.FetchAbout
import Messages exposing (AnyMsg(..))
import Models.Ui.Notifications exposing (Errors)
import Models.Ui.LoginForm exposing (LoginForm, emptyLoginForm)
-- import Updates.UpdateTemplates exposing (updateTemplates)
import Views.Header
import Views.Body
import Views.Notifications

-- TODO what type of submessages do I want to have?
-- - Messages changing resources
-- - Error messages
-- - Messages changing the view
-- so one message per entry in my model? that means that not every single thing should define its own Msg type otherwise it will get crazy

type alias Model =
  { aboutInfo : Maybe AboutInfo
  -- , templates : List Template
  , errors : Errors
  , loginForm : LoginForm
  , loggedIn : Maybe UserInfo
  -- , expandedNewInstanceForms : Set TemplateId
  }

initialModel : Model
initialModel =
  { aboutInfo = Nothing
  -- , templates = []
  , errors = []
  , loginForm = emptyLoginForm
  , loggedIn = Nothing
  -- , expandedNewInstanceForms = Set.empty
  }

init : ( Model, Cmd AnyMsg )
init =
  ( initialModel
  , Cmd.batch
    [ Cmd.map UpdateAboutInfoMsg Commands.FetchAbout.fetchAbout
    -- , Cmd.map FetchTemplatesMsg Commands.FetchTemplates.fetchTemplates
    ]
  )

update : AnyMsg -> Model -> ( Model, Cmd AnyMsg )
update msg model =
  case msg of
    -- FetchTemplatesMsg subMsg ->
      -- let (newTemplates, cmd) =
      --   updateTemplates subMsg model.templates
      -- in
      --   ({ model | templates = newTemplates }
      --   , cmd
      --   )
    UpdateAboutInfoMsg subMsg ->
      let (newAbout, cmd) =
        updateAboutInfo subMsg model.aboutInfo
      in
        ({ model | aboutInfo = newAbout }
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
    [ class "container" ]
    [ Views.Header.view model.aboutInfo model.loginForm
    , Views.Notifications.view model.errors
    , text (toString model.loggedIn)
    -- ,  Html.map ViewsBodyMsg Views.Body.view model.templatesModel
    ]

subscriptions : Model -> Sub AnyMsg
subscriptions model =
  Sub.none

main =
  program
    { init = init
    , view = view
    , update = update
    , subscriptions = subscriptions
    }
