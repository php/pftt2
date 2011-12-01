
# LATER testing the MSSQL driver is exactly the normal usage of PFTT
#        just only need the MsSQL scenario*
#        just run the unit tests as PHPTs**
#        may want to run on more linux SKUs (at least once its stable)
#        should run on lots of PHP builds(at least 6: TS and NTS builds of 5.3.8, 5.3.9, 5.4.0)
#             not just one or two (like we normally do)
#
# * - so the MSSQL server is setup and the driver is loaded into php
# ** - so anyone in the PHP community can test the MSSQL driver
#          - enables PFTT to demonstrate how well it works to users
#
class Mssql
  def scn_name
    'db_mssql'
  end
end