package org.fedoraproject.mobile

import scala.language.existentials

import Implicits._

import android.app.{ Activity, Fragment }
import android.content.{ Context, Intent }
import android.os.{ Build, Bundle }
import android.preference.PreferenceManager
import android.support.v4.app.ActionBarDrawerToggle
import android.support.v4.view.GravityCompat
import android.util.Log
import android.view.{ LayoutInflater, MenuItem, View, ViewGroup }
import android.widget.AbsListView.OnScrollListener
import android.widget.{ AdapterView, AbsListView, ArrayAdapter, TextView, Toast }

import scalaz._, Scalaz._
import scalaz.concurrent.Promise

import java.util.ArrayList // TODO: Do something about this.

sealed trait Delegation {
  def d: Class[_]
  def icon: Int
  def name: String
}
sealed case class ActivityDelegation(
  override val d: Class[_ <: Activity],
  override val icon: Int,
  override val name: String) extends Delegation
sealed case class FragmentDelegation(
  override val d: Class[_ <: Fragment],
  override val icon: Int,
  override val name: String) extends Delegation

sealed class NavAdapter(
  context: Context,
  resource: Int,
  delegations: List[Delegation])
  extends ArrayAdapter[String](context, resource, delegations.map(_.name).toArray) {
    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val delegation: Delegation = delegations(position)

      val layout = LayoutInflater.from(context)
        .inflate(R.layout.drawer_list_item, parent, false)
        .asInstanceOf[TextView]

      layout.tap { obj =>
        obj.setCompoundDrawablesWithIntrinsicBounds(delegation.icon, 0, 0, 0)
        obj.setText(delegation.name)
      }

      layout
    }
  }

class MainActivity extends util.Views {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.navdrawer)

    val navMap: List[Delegation] =
      List(
        ActivityDelegation(
          classOf[StatusActivity],
          R.drawable.ic_status,
          getString(R.string.infrastructure_status)),
        ActivityDelegation(
          classOf[PackageSearchActivity],
          R.drawable.ic_search,
          getString(R.string.package_search)),
        ActivityDelegation(
          classOf[BadgesLeaderboardActivity],
          R.drawable.ic_badges,
          getString(R.string.badges_leaderboard)),
        ActivityDelegation(
          classOf[FedmsgRegisterActivity],
          R.drawable.ic_fedmsg,
          getString(R.string.register_fmn)),
        ActivityDelegation(
          classOf[PreferencesActivity],
          R.drawable.ic_preferences,
          getString(R.string.preferences)),
        ActivityDelegation(
          classOf[UserActivity],
          R.drawable.ic_preferences,
          "Profile UI Demo")
      )

    val title = getTitle

    val drawerLayout = findView(TR.drawer_layout)
    val drawerToggle = new ActionBarDrawerToggle(
      this,
      drawerLayout,
      R.drawable.ic_drawer,
      R.string.open,
      R.string.close) {
      override def onDrawerClosed(view: View): Unit = {
        super.onDrawerClosed(view)
        getActionBar.setTitle(title)
        invalidateOptionsMenu
      }
      override def onDrawerOpened(view: View): Unit = {
        super.onDrawerOpened(view)
        getActionBar.setTitle(title)
        invalidateOptionsMenu
      }
    }

    drawerLayout.setDrawerListener(drawerToggle)
    drawerToggle.syncState

    val drawerList = findView(TR.left_drawer)
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
    drawerList.setAdapter(
      new NavAdapter(
        this,
        android.R.layout.simple_list_item_1,
        navMap))

    drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
        navMap(position) match {
          case ActivityDelegation(c, i, s) => {
            val intent = new Intent(MainActivity.this, c)
            drawerLayout.closeDrawer(drawerList)
            startActivity(intent)
          }
          case FragmentDelegation(c, i, s) => {
            val fragment = c.newInstance
            val fragmentManager = getFragmentManager
            fragmentManager.beginTransaction
              .replace(R.id.content_frame, fragment)
              .commit()
          }

          drawerList.setItemChecked(position, true)
          getActionBar.setTitle(s)
          drawerLayout.closeDrawer(drawerList)
        }
      }
    })

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val checkUpdates = sharedPref.getBoolean("check_updates", true)

    // If the user hasn't disabled updates...
    if (checkUpdates) {
      val versionCompare: Promise[String \/ Boolean] = Updates.compareVersion(this)
      versionCompare map {
        case \/-(b) if b == true =>
          Log.e("MainActivity", "We're up to date!")
        case \/-(b) if b == false =>
          runOnUiThread(Updates.presentDialog(MainActivity.this))
        case -\/(err) =>
          Log.e("MainActivity", err.toString)
      }
    }
  }

  /*
  override def onPrepareOptionsMenu(menu: Menu): Boolean {
    val drawerOpen: Boolean = mDrawerLayout.isDrawerOpen(mDrawerList)
    menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
    return super.onPrepareOptionsMenu(menu);
  }
  */
}
