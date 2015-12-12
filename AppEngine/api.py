__author__ = 'simon'

import endpoints
from protorpc import remote
from protorpc import messages
from models import User
from firebase_token_generator import create_token
from google.appengine.api import urlfetch
import logging
import math
import json

FIREBASE_TOKEN = "Atllp9TRmIn1igPwvgy1cCMmCpkG5MLjkfjh1XnB"
GSM_TOKEN = "AIzaSyArh0wY2lkLZeobveexebqwigoqCb7_Q-o"
EARTH_RADIUS = 6371


class Location(messages.Message):
    long = messages.FloatField(1, required=True)
    lat = messages.FloatField(2, required=True)


class RegistrationRequest(messages.Message):
    token = messages.StringField(1, required=True)
    user_id = messages.StringField(2, required=True)


class RegistrationResponse(messages.Message):
    token = messages.StringField(1, required=True)


class AlarmRequest(messages.Message):
    user_id = messages.StringField(1, required=True)
    location = messages.MessageField(Location, 2, required=True)


class AlarmResponse(messages.Message):
    code = messages.IntegerField(1, required=True)
    msg = messages.StringField(2, required=True)
    node = messages.StringField(3)


@endpoints.api(name='savemyass', version='v1')
class SaveMyAssAPI(remote.Service):
    """SaveMyAss API"""

    @endpoints.method(RegistrationRequest, RegistrationResponse,
                      path='register', http_method='POST',
                      name='savemyass.register')
    def register(self, request):

        user_query_results = User.query(User.user_id == request.user_id).fetch()
        user_exists = False
        for u in user_query_results:
            u.token = request.token
            u.put()
            user_exists = True

        if not user_exists:
            User(user_id=request.user_id, token=request.token).put()

        auth_payload = {"uid": request.user_id}
        token = create_token(FIREBASE_TOKEN, auth_payload)
        return RegistrationResponse(token=token)

    @endpoints.method(AlarmRequest, AlarmResponse,
                      path='alarm', http_method='POST',
                      name='savemyass.alarm')
    def alarm(self, request):
        longitude = request.location.long
        latitude = request.location.lat
        hash = "asdf"  # TODO: create the hash with geohash library
        user_id = request.user_id

        # Step 0: Check if allowed to make request
        user_query_results = User.query(User.user_id == user_id).fetch()
        user = None
        for u in user_query_results:
            user = u

        if user is None:
            return AlarmResponse(code=300,
                                 msg="Der Nutzer ist nicht registriert oder die Zugangsdaten sind nicht korrekt.")



        # Step 1: Query firebase for location
        location_url = 'https://savemya.firebaseio.com/locations.json?orderBy="g"&startAt="a"'
        loc_result = urlfetch.fetch(location_url)
        if loc_result.status_code != 200:
            return AlarmResponse(code=500, msg="Firebase Server konnte nicht angesprochen werden.")


        # Step 2: Extract the possible helpers and their GCM Tokens
        helpers = []
        helper_token = []

        for key in json.loads(loc_result.content):
            if not key == user_id:
                helpers.append(key)

        if len(helpers) < 1:
            return AlarmResponse(code=300, msg="Es wurden keine Helfer in der Nähe gefunden.")


        # Step 3: create the Firebase Alarm Object
        url = 'https://savemya.firebaseio.com/alarms.json?auth=' + FIREBASE_TOKEN
        alarm_data = {"pin": {"user_id": user_id, "location": {"g": hash, "l": [longitude, latitude]}}, "helpers": []}
        alarm_result = urlfetch.fetch(url=url, method=urlfetch.POST, payload=json.dumps(alarm_data))
        if alarm_result.status_code != 200:
            return AlarmResponse(code=500, msg="Firebase Server konnte nicht angesprochen werden.")
        logging.log(logging.INFO, alarm_result.content)
        node = json.loads(alarm_result.content)["name"]


        # Step 4: send the GCM Message to the possible Helpers
        users = User.query(User.user_id.IN(helpers)).fetch()
        for user in users:
            helper_token.append(user.token)

        headers = {"Authorization": "key=" + GSM_TOKEN, "Content-Type": "application/json"}
        data = {"data": {"longitude": longitude, "latitude": latitude, "node": node}, "registration_ids": helper_token}
        gsm_result = urlfetch.fetch(url="https://gcm-http.googleapis.com/gcm/send", method=urlfetch.POST,
                                    headers=headers, payload=json.dumps(data))
        if gsm_result.status_code != 200:
            return AlarmResponse(code=500, msg="GCM Nachricht konnte nicht versendet werden.")

        response = json.loads(gsm_result.content)
        if response["success"] < 1:
            return AlarmResponse(code=300, msg="Es wurden keine Helfer in der Nähe gefunden.")

        return AlarmResponse(code=200, node=node,
                             msg="Es wurden " + str(response["success"]) + " Personen benachrichtigt.")


api = endpoints.api_server([SaveMyAssAPI])
