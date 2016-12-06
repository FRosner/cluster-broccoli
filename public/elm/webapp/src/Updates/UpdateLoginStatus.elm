module Updates.UpdateLoginStatus exposing (updateLoginStatus)

import Updates.Messages exposing (UpdateLoginStatusMsg(..), UpdateLoginFormMsg(..), UpdateErrorsMsg(..))
import Messages exposing (AnyMsg(..))
import Models.Resources.UserInfo exposing (UserInfo)
import Utils.CmdUtils exposing (cmd)
import Debug

updateLoginStatus : UpdateLoginStatusMsg -> Maybe UserInfo -> (Maybe UserInfo, Cmd AnyMsg)
updateLoginStatus message oldLoginStatus =
  case message of
    FetchLogin (Ok loggedInUser) ->
      ( Just loggedInUser
      , Cmd.none -- TODO request about again on successful login?
      )
    FetchLogin (Err error) ->
      ( oldLoginStatus
      , (cmd (UpdateLoginFormMsg FailedLoginAttempt))
      )
    FetchLogout (Ok string) ->
      ( Nothing
      , Cmd.none
      )
    FetchLogout (Err error) ->
      ( oldLoginStatus
      , ( cmd ( UpdateErrorsMsg ( AddError "Logout failed." ) ) )
      )
    ResumeExistingSession loggedInUser ->
      ( Just loggedInUser
      , Cmd.none
      )
