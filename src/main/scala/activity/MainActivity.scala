package org.fedoraproject.mobile

import scala.language.existentials

import Implicits._

import android.app.{ Activity, Fragment }
import android.content.{ Context, Intent }
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActionBarDrawerToggle
import android.support.v4.view.GravityCompat
import android.util.Log
import android.view.{ LayoutInflater, MenuItem, View, ViewGroup }
import android.widget.{ AdapterView, ArrayAdapter, TextView }

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.effect.IO

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

      layout.setCompoundDrawablesWithIntrinsicBounds(delegation.icon, 0, 0, 0)
      layout.setText(delegation.name)
      layout
    }
  }

class MainActivity extends util.Views {
  // Blech - can we do something about this?
  private var drawerToggle: Option[ActionBarDrawerToggle] = None

  override def onPostCreate(bundle: Bundle): Unit = {
    super.onPostCreate(bundle)
    setContentView(R.layout.navdrawer)

    val navMap: List[Delegation] =
      List(
        FragmentDelegation(
          classOf[FedmsgNewsfeedFragment],
          R.drawable.ic_status,
          "Newsfeed"),
        FragmentDelegation(
          classOf[StatusFragment],
          R.drawable.ic_status,
          getString(R.string.infrastructure_status)),
        ActivityDelegation(
          classOf[PackageSearchActivity],
          R.drawable.ic_search,
          getString(R.string.package_search)),
        FragmentDelegation(
          classOf[BadgesLeaderboardFragment],
          R.drawable.ic_badges,
          getString(R.string.badges_leaderboard)),
        ActivityDelegation(
          classOf[FedmsgRegisterActivity],
          R.drawable.ic_fedmsg,
          getString(R.string.register_fmn)),
        ActivityDelegation(
          classOf[UserActivity],
          R.drawable.ic_preferences,
          "Profile UI Demo"),
        FragmentDelegation(
          classOf[PreferencesFragment],
          R.drawable.ic_preferences,
          getString(R.string.preferences))
      )

    val title = getTitle

    val drawerLayout = findView(TR.drawer_layout)
    drawerToggle = Some(
      new ActionBarDrawerToggle(
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
    )

    val drawerList = findView(TR.left_drawer)
    getActionBar.setDisplayHomeAsUpEnabled(true)
    getActionBar.setHomeButtonEnabled(true)
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
    drawerToggle.map(drawerLayout.setDrawerListener)
    drawerToggle.map(_.syncState)

    drawerList.setAdapter(
      new NavAdapter(
        this,
        android.R.layout.simple_list_item_1,
        navMap))

    drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
        spawn(navMap(position)).unsafePerformIO // TODO
        drawerList.setItemChecked(position, true)
        drawerLayout.closeDrawer(drawerList)
      }
    })

    // Default fragment
    spawn(navMap.head).unsafePerformIO // TODO

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val checkUpdates = sharedPref.getBoolean("check_updates", true)

    // If the user hasn't disabled updates...
    if (checkUpdates) {
      // If the most recent build failed, there's no point in doing anything
      // else.
      val x: Task[Unit] = Updates.getJenkinsLastBuildStatus >>=
        ((x: String \/ Updates.JenkinsBuildStatus) => Task.delay(x match {
          // Left happens if the JSON parse from Jenkins fails.
          case -\/(err)                    => { Log.e("MainActivity", err); () }
          case \/-(Updates.JenkinsFailure) => { Log.v("MainActivity", "Last Jenkins build failed. Skipping."); () }
          case \/-(Updates.JenkinsSuccess) => {
            Updates.compareVersion(this) >>=
              ((y: String \/ Boolean) => Task.delay(y match {
                case \/-(true)  => { Log.v("MainActivity", "Already up to date"); () }
                case \/-(false) => { runOnUiThread(Updates.presentDialog(MainActivity.this)); () }
                case -\/(err)   => { Log.e("MainActivity", err); () }
              }))
              ()
          }
        }))
      x.runAsync(_ => ())
    }
  }

  private def spawn(x: Delegation): IO[Unit] = IO {
    x match {
      case ActivityDelegation(c, i, s) => {
        val intent = new Intent(MainActivity.this, c)
        getActionBar.setTitle(s)
        startActivity(intent)
      }
      case FragmentDelegation(c, i, s) => {
        getActionBar.setTitle(s)
        val fragment = c.newInstance
        val fragmentManager = getFragmentManager
        fragmentManager.beginTransaction
         .replace(R.id.content_frame, fragment)
         .commit()
      }
    }
    ()
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    drawerToggle match {
      case Some(t) =>
        if (t.onOptionsItemSelected(item))
          true
        else
          super.onOptionsItemSelected(item)
      case None => super.onOptionsItemSelected(item)
    }
}
