## scandium-dtls-server
A DTLS server test program based on [Scandium/Californium][scandium] which is adapted from the example given in their repository but has a few more knobs that one can toy with. 

This program was used to test the Scandium DTLS server implementations as part of the state fuzzing work published in [USENIX 20][usenix]. 
As we extended the program for clients, we moved development to a [new repository][new-scandium].

At the time of development, there were two versions of Scandium available, 1.x.y, and 2.x.y. 
The APIs differ slightly between the two, hence we wrote separate applications, one for each version. 
You can find these applications in the branches of this repo.
The master is set to 2.x.y (since it is the most recent).


[usenix]:https://www.usenix.org/conference/usenixsecurity20/presentation/fiterau-brostean
[scandium]:https://github.com/eclipse/californium/tree/master/scandium-core
[new-scandium]:https://github.com/assist-project/scandium-dtls-examples/
