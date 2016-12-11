module Main exposing (main)

import Html exposing (..)
import Html.Attributes exposing (..)
import Set exposing (Set)
import Models.Resources.Template exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Updates.UpdateAboutInfo exposing (updateAboutInfo)
import Updates.UpdateErrors exposing (updateErrors)
import Updates.UpdateLoginForm exposing (updateLoginForm)
import Updates.UpdateLoginStatus exposing (updateLoginStatus)
import Updates.UpdateBodyView exposing (updateBodyView)
import Updates.Messages exposing (UpdateAboutInfoMsg(..), UpdateLoginStatusMsg(..), UpdateErrorsMsg(..))
import Commands.FetchAbout
import Messages exposing (AnyMsg(..))
import Models.Ui.Notifications exposing (Errors)
import Models.Ui.LoginForm exposing (LoginForm, emptyLoginForm)
import Views.Header
import Views.Body
import Views.Notifications
import WebSocket
import Dict exposing (Dict)

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
  , authEnabled : Maybe Bool
  , templates : List Template
  , expandedTemplates : Set TemplateId
  , instances : List Instance
  , services : Dict InstanceId (List Service)
  -- , expandedNewInstanceForms : Set TemplateId
  }

template1 =
  Template
    "curl"
    "This is a very curly template."
    "chj3kc67"
    [ "id"
    , "url"
    ]
    ( Dict.fromList
      [ ( "id", ParameterInfo "id" Nothing Nothing )
      , ( "url", ParameterInfo "url" (Just "http://localhost:8000") Nothing )
      ]
    )

template2 =
  Template
    "http-server"
    "Use this one to serve awesome HTTP responses based on a directory. The directory will be the one you are currently working in and it is a lot of fun to use this template."
    "dsadjda4"
    [ "id"
    , "password"
    ]
    ( Dict.fromList
      [ ( "id", ParameterInfo "id" Nothing Nothing )
      , ( "password", ParameterInfo "password" Nothing (Just True) )
      ]
    )

initialModel : Model
initialModel =
  { aboutInfo = Nothing
  -- , templates = []
  , errors = []
  , loginForm = emptyLoginForm
  , loggedIn = Nothing
  , authEnabled = Nothing
  , expandedTemplates = Set.empty
  , templates =
    [ template1
    , template2
    ]
  , instances =
    [ Instance
        "curl-1"
        template1
        ( Dict.fromList
          [ ("id", "curl-1")
          , ("url", "http://localhost:9000")
          ]
        )
    , Instance
        "http-server-1"
        template2
        ( Dict.fromList
          [ ("id", "http-server-1")
          , ("password", "secret")
          ]
        )
    , Instance
        "http-server-2"
        template2
        ( Dict.fromList
          [ ("id", "http-server-2")
          , ("password", "secret2")
          ]
        )
    ]
  , services =
    ( Dict.fromList
      [ ( "http-server-2"
        , [ Service "http-server-2-ui" "http" "localhost" 9000 Passing
          , Service "http-server-2-api" "https" "localhost" 9001 Failing
          ]
        )
      , ( "http-server-1"
        , [ Service "http-server-1-ui" "http" "localhost" 9000 Unknown
          , Service "http-server-1-api" "https" "localhost" 9001 Unknown
          ]
        )
      ]
    )

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
      let ((newAbout, newAuthEnabled), cmd) =
        updateAboutInfo subMsg model.aboutInfo
      in
        ( { model
          | aboutInfo = newAbout
          , authEnabled = newAuthEnabled
          }
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
      let (newExpandedTemplates, cmd) =
        updateBodyView subMsg model.expandedTemplates
      in
        ({ model | expandedTemplates = newExpandedTemplates }
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
            model.expandedTemplates
            model.instances
            model.services
        )
    , text (toString model)
    ]

subscriptions : Model -> Sub AnyMsg
subscriptions model =
  Sub.map
    UpdateErrorsMsg
    -- TODO I need a module to handle the websocket string messages and parse them into JSON somehow
    -- TODO cut the websocket connection on logout
    ( WebSocket.listen "ws://localhost:9000/ws" AddError )

main =
  program
    { init = init
    , view = view
    , update = update
    , subscriptions = subscriptions
    }
