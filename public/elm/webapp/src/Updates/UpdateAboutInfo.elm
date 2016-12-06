module Updates.UpdateAboutInfo exposing (updateAboutInfo)

import Updates.Messages exposing (UpdateAboutInfoMsg(..), UpdateErrorsMsg(..), UpdateLoginStatusMsg(..))
import Models.Resources.AboutInfo exposing (AboutInfo)
import Messages exposing (AnyMsg(..))
import Utils.CmdUtils exposing (cmd)
import Http exposing (Error(..))

updateAboutInfo : UpdateAboutInfoMsg -> Maybe AboutInfo -> ((Maybe AboutInfo, Maybe Bool), Cmd AnyMsg)
updateAboutInfo message oldAboutInfo =
  case message of
    FetchAbout (Ok newAboutInfo) ->
      (
        ( Just newAboutInfo
        , Just newAboutInfo.authInfo.enabled
        )
      , ( cmd ( UpdateLoginStatusMsg (ResumeExistingSession newAboutInfo.authInfo.userInfo) ) )
      )
    FetchAbout (Err error) ->
      case error of
        BadStatus request ->
          if (request.status.code == 403) then
            (
              ( Nothing
              , Just True
              )
            , Cmd.none
            )
          else
            (
              ( Nothing
              , Nothing
              )
            , ( cmd
                ( UpdateErrorsMsg
                  ( AddError
                    ( String.concat
                        [ "Cluster Broccoli not reachable: "
                        , (toString request.status.code)
                        , " ("
                        , (toString request.status.message)
                        , ")"
                        ]
                    )
                  )
                )
              )
            )
        _ ->
          (
            ( Nothing
            , Nothing
            )
          , (cmd (UpdateErrorsMsg (AddError "Fetching 'about' failed.")))
          )
-- errorToString : Http.Error -> String
-- errorToString error =
--   case error of
--     BadStatus request ->
--       String.concat
--         [ "Fetching information about Cluster Broccoli failed: "
--         , ( toString request.status.code )
--         , " ("
--         , request.status.message
--         , ")"
--         ]
--     _ -> toString error
