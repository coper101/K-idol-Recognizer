# Files imported into python directory
# - labels.pickle
# - encodings.pickle
# - Kpop_Idols_210321_CSV.csv

import os
import numpy as np
import pickle
import cv2
import base64
import face_recognition
import pandas as pd


def detect_face_fr(image_data):
    # Decode Image Data and Convert To Numpy
    decode_data = base64.b64decode(image_data)
    np_data = np.fromstring(decode_data, np.uint8)
    image_4_channels = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    img = image_4_channels[...,:3]

    labels, encodings = retrieve_encodings_labels()

    # Using Face Recognition
    imgSmall = cv2.resize(img, (0, 0), None, 0.25, 0.25)
    imgSmall = cv2.cvtColor(imgSmall, cv2.COLOR_BGR2RGB)

    facesCurFrame = face_recognition.face_locations(imgSmall)
    encodesCurFrame = face_recognition.face_encodings(imgSmall, facesCurFrame)

    # Loop Faces for Current Frame
    idsBbox = []
    for encodeFace, faceLoc in zip(encodesCurFrame, facesCurFrame):
        matches = face_recognition.compare_faces(encodings, encodeFace)
        faceDis = face_recognition.face_distance(encodings, encodeFace)
        matchIndex = np.argmin(faceDis)

        if matches[matchIndex]:
            # (1) label
            temp = []
            label = labels[matchIndex]
            temp.append(extractId(label))
            # (2) bounding box axis
            y1, x2, y2, x1 = faceLoc
            y1, x2, y2, x1 = y1*4, x2*4, y2*4, x1*4
            temp.extend([x1, x2, y1, y2])
            # add to nested list
            idsBbox.append(temp)

    print(idsBbox)

    return idsBbox

def extractId(label):
    id = label.split('_')[0]
    return id

def retrieve_encodings_labels():

    lblFileName = os.path.join(os.path.dirname(__file__), "labels.pickle")
    lblFile = open(lblFileName, "rb")
    labels = pickle.load(lblFile)
    lblFile.close()

    encFileName = os.path.join(os.path.dirname(__file__), "encodings.pickle")
    encFile = open(encFileName, "rb")
    encodings = pickle.load(encFile)
    encFile.close()

    return labels, encodings

def get_idol_profile(id):

    dataFileName = os.path.join(os.path.dirname(__file__), "Kpop_Idols_210321_CSV.csv")
    dataFileNameUser = os.path.join(os.environ["HOME"], "Kpop_Idols_210321_CSV.csv")

    # read from? home user directory OR python directory
    if os.path.exists(dataFileNameUser):
        data = pd.read_csv(dataFileNameUser)
    else:
        data = pd.read_csv(dataFileName)

    fltr = data['Id'] == int(id)
    idol = data.loc[fltr]

    headerVal = data.columns.values.tolist()
    try:
        idolVal = idol.values[0].tolist()
    except:
        idolVal = []

    idolProfile = {}

    if len(headerVal) > 0 and len(idolVal) > 0:
        for i in range(len(headerVal)):
            idolProfile[headerVal[i]] = idolVal[i]

    return idolProfile


def getType(value):
    return type(value)


def update_favorite(id, isFave):

    # proper bool string
    properValue = isFave.title()

    # valid bool string > update data frame and overwrite csv
    if properValue == 'True' or properValue == 'False':
        # convert to bool
        boolValue = eval(properValue)

        dataFileName = os.path.join(os.path.dirname(__file__), "Kpop_Idols_210321_CSV.csv")
        dataFileNameUser = os.path.join(os.environ["HOME"], "Kpop_Idols_210321_CSV.csv")
        # read from? home user directory OR python directory
        if os.path.exists(dataFileNameUser):
            data = pd.read_csv(dataFileNameUser)
        else:
            data = pd.read_csv(dataFileName)

        # update fave value of idol
        fltr = data['Id'] == int(id)
        data.loc[fltr, 'Favorite'] = boolValue

        # save
        if os.path.exists(dataFileNameUser):
            data.to_csv(dataFileNameUser, index=False)
        else:
            data.to_csv(dataFileName, index=False)

        return True

    return False


def save_idols_data_to_home():

    dataFileName = os.path.join(os.path.dirname(__file__), "Kpop_Idols_210321_CSV.csv")
    data = pd.read_csv(dataFileName)

    # save data to home: /data/user/0/com.daryl.kidolrecognizer/files
    homePath = os.environ["HOME"]
    newFilePath = os.path.join(homePath, "Kpop_Idols_210321_CSV.csv")
    data.to_csv(newFilePath, index=False)

    exist = os.path.exists(newFilePath)

    return exist


def check_idols_data_from_home():
    homePath = os.environ["HOME"]
    newFilePath = os.path.join(homePath, "Kpop_Idols_210321_CSV.csv")
    exist = os.path.exists(newFilePath)
    return exist

def get_favorite_idols():

    faveIdols = []

    dataFileNameUser = os.path.join(os.environ["HOME"], "Kpop_Idols_210321_CSV.csv")
    exist = os.path.exists(dataFileNameUser)
    print('Home CSV Exist? ', exist)

    if exist:
        # read csv
        data = pd.read_csv(dataFileNameUser)
        # get favorite idol rows
        faveFltr = data['Favorite'] == True
        idols = data.loc[faveFltr]
        # get only stage name & group name
        idols2Cols = idols[['Id', 'Stage Name', 'Group Name']]
        # to list
        idols2ColsVal = idols2Cols.values.tolist()

        faveIdols = idols2ColsVal

    print(faveIdols)

    return faveIdols


def get_all_idols():

    idolsList = []

    dataFileNameUser = os.path.join(os.environ["HOME"], "Kpop_Idols_210321_CSV.csv")
    exist = os.path.exists(dataFileNameUser)
    print('Home CSV Exist? ', exist)

    if exist:
        # read csv
        data = pd.read_csv(dataFileNameUser)
        # get only stage name & group name
        allIdols = data[['Stage Name', 'Group Name']]
        # to list
        allIdolsVal = allIdols.values.tolist()

        idolsList = allIdolsVal

    return idolsList




