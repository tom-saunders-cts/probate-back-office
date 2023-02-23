#!/bin/bash
## Usage: ./idam-service-token.sh [microservice_name]
##
## Options:
##    - microservice_name: Name of the microservice. Default to `ccd_gw`.
##
## Returns a valid IDAM service token for the given microservice.

microservice="${1:-ccd_gw}"
URL=${S2S_API_URL:-http://rpe-service-auth-provider-aat.service.core-compute-aat.internal}
URL=${S2S_API_URL:-http://localhost:4502}

curl ${CURL_OPTS} \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"microservice":"'${microservice}'"}' \
  ${URL}/testing-support/lease
