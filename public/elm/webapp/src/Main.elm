module Main exposing (main)

import Html exposing (..)
import Html.Attributes exposing (..)
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId, Template)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Updates.UpdateAboutInfo exposing (updateAboutInfo)
import Updates.UpdateErrors exposing (updateErrors)
import Updates.UpdateLogin exposing (updateLogin)
import Updates.Messages exposing (UpdateAboutInfoMsg)
import Commands.FetchAbout
import Messages exposing (AnyMsg(..))
import Models.Ui.Notifications exposing (Errors)
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
  , loggedIn : Bool
  -- , expandedNewInstanceForms : Set TemplateId
  }

initialModel : Model
initialModel =
  { aboutInfo = Nothing
  -- , templates = []
  , errors = []
  , loggedIn = False
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
    UpdateErrorsMsg subMsg ->
      let (newErrors, cmd) =
        updateErrors subMsg model.errors
      in
        ({ model | errors = newErrors }
        , cmd
        )
    UpdateLoginMsg subMsg ->
      let (newLoggedIn, cmd) =
        updateLogin subMsg model.loggedIn
      in
        ({ model | loggedIn = newLoggedIn }
        , cmd
        )
    NoOp -> (model, Cmd.none)

view : Model -> Html AnyMsg
view model =
  div
    [ class "container" ]
    [ Views.Header.view model.aboutInfo
    , Views.Notifications.view model.errors
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
