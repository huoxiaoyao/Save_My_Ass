__author__ = 'simon'

import endpoints
from protorpc import remote
from protorpc import messages

class RegistrationRequest(messages.Message):
    id = messages.StringField(1, required=True)
    salt = messages.StringField(2, required=True)

class RegistrationResponse(messages.Message):
    node = messages.StringField(1, required=True)

@endpoints.api(name='savemyass', version='v1')
class SaveMyAssAPI(remote.Service):
    """SaveMyAss API"""

    @endpoints.method(RegistrationRequest, RegistrationResponse,
                      path='register', http_method='POST',
                      name='savemyass.register')
    def register(self, request):
        return RegistrationResponse(node="bla")


api = endpoints.api_server([SaveMyAssAPI])
