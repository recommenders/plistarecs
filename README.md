plistarecs
==========

Set of recommenders for the [Plista News Recommendation Challenge](https://sites.google.com/site/newsrec2013/challenge).

The recommenders use modified versions of the Plista contest client implementation by [@matip](http://twitter.com/matip) available at
[plistacontest](https://github.com/matip/plistacontest) (forked [here](https://github.com/recommenders/plistacontest)) and the Plista Challenge client implementation by [@torbenbrodt](https://twitter.com/torbenbrodt) available [here](https://github.com/plista/orp-sdk-java)

Implemented by [recommenders.net](http://www.recommenders.net)

##Instructions
###Prerequisites
* java 7
* maven

###Running a recommender
1. Get the client
  1. ```git clone https://github.com/recommenders/plistaclient```
  2. ```cd plistaclient```
  3. ```mvn clean install```
2. Get the recommenders
  1. ```git clone https://github.com/recommenders/plistarecs```
  2. ```cd plistarecs```
  3. ```vim plista.properties``` to configure a r[ecommender](https://github.com/recommenders/plistarecs/tree/master/src/main/java/net/recommenders/plista/rec) and port
  4. ```mvn clean package; ./run.sh``` (preferably in a ```screen```)
  5. Check if your server is running at http://yourhost:yourport
3. Register account at [ORP](http://orp.plista.com) and connect to your server
4. Win the [prizes](https://sites.google.com/site/newsrec2013/challenge#TOC-Prizes)

###Additional info
The recommenders will fail initially as there is no data provided in the repository. As the recommender receives impressions from Plista it will create data files in the folder it lives. This data is used for recommendation.
Note that data files grow fast, you will need plenty of gigabytes of storage for this.
