port module Main exposing (..)

import Json.Encode exposing (Value)
import Test exposing (describe)
import Test.Runner.Node exposing (TestProgram, run)
import Utils.DictUtilsSuite as DictUtilsSuite
import Utils.ParameterUtilsSuite as ParameterUtilsSuite
import Views.BodySuite as BodySuite
import Views.FooterSuite as FooterSuite
import Views.HeaderSuite as HeaderSuite
import Views.LogUrlSuite as LogUrlSuite
import Views.NotificationsSuite as NotificationsSuite
import Views.PeriodicRunsViewSuite as PeriodicRunsViewSuite


main : TestProgram
main =
    run
        emit
        (describe
            "Cluster Broccoli UI Tests"
            [ ParameterUtilsSuite.tests
            , DictUtilsSuite.tests
            , HeaderSuite.tests
            , NotificationsSuite.tests
            , BodySuite.tests
            , FooterSuite.tests
            , LogUrlSuite.tests
            , PeriodicRunsViewSuite.tests
            ]
        )


port emit : ( String, Value ) -> Cmd msg
