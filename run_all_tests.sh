#!/usr/bin/env bash
sbt clean compile coverage test dependencyUpdates coverageReport
