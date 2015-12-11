__author__ = 'simon'

from google.appengine.ext import ndb

class User(ndb.Model):
    user_id = ndb.StringProperty(required=True)
    token = ndb.StringProperty(required=True)