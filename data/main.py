
import os
import json
from pymongo import MongoClient


# Making Connection
uri = "mongodb+srv://kian:mk89081315@cluster0.ekorb.mongodb.net/?retryWrites=true&w=majority"
myclient = MongoClient(uri);
db = myclient["HAKEE-database"]
col = db["Opportunity-dataset"]
Collection = db["data"]

dirs = os.listdir("./")
for dir in dirs:
    if os.path.isfile(dir) and dir.endswith(".json"):
        with open(dir) as file:
            file_data = json.load(file)
        col.insert_one(file_data)



