plistarecs
==========

Set of recommenders for the [Plista News Recommendation Challenge](https://sites.google.com/site/newsrec2013/challenge).

The recommenders use a modified version the Plista contest client implementation by [@matip](http://twitter.com/matip) available at
[plistacontest](https://github.com/matip/plistacontest) (forked [here](https://github.com/recommenders/plistacontest))

Implemented by [recommenders.net](http://www.recommenders.net)

##Instructions
###Prerequisites
* java 7
* maven

###Running the recommender
1. Get the client
  1. ```git clone https://github.com/recommenders/plistaclient```
  2. ```cd plistaclient```
  3. ```mvn clean install```
2. Get the recommenders
  1. ```git clone https://github.com/recommenders/plistarecs```
  2. ```cd plistarecs```
  3. ```vim plista.properties``` to configure recommender and port
  4. ```mvn clean package& ./run.sh``` (preferably in a ```screen```)
3. Win the [prizes](https://sites.google.com/site/newsrec2013/challenge#TOC-Prizes)

###Additional info
The recommenders will fail initially as there is no data provided in the repository. As the recommender receives impressions from Plista it will create data files in the folder it lives. This data is used for recommendation.
Note that data files grow fast, you will need plenty of gigabytes of storage for this.
