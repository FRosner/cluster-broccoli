port module Main exposing (..)

import Utils.MaybeUtilsSuite as MaybeUtilsSuite
import Views.FooterSuite as FooterSuite

import Test exposing (describe)

import Test.Runner.Node exposing (run, TestProgram)

import Json.Encode exposing (Value)

main : TestProgram
main =
  run
    emit
    ( describe
      "Cluster Broccoli UI Tests"
      [ MaybeUtilsSuite.tests
      , FooterSuite.tests
      ]
    )

port emit : ( String, Value ) -> Cmd msg
