port module Main exposing (..)

import Utils.ParameterUtilsSuite as ParameterUtilsSuite
import Views.HeaderSuite as HeaderSuite
import Views.NotificationsSuite as NotificationsSuite
import Views.BodySuite as BodySuite
import Views.FooterSuite as FooterSuite
import Test exposing (describe)
import Test.Runner.Node exposing (run, TestProgram)
import Json.Encode exposing (Value)


main : TestProgram
main =
    run
        emit
        (describe
            "Cluster Broccoli UI Tests"
            [ ParameterUtilsSuite.tests
            , HeaderSuite.tests
            , NotificationsSuite.tests
            , BodySuite.tests
            , FooterSuite.tests
            ]
        )


port emit : ( String, Value ) -> Cmd msg
